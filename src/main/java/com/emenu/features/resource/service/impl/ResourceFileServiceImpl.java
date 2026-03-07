package com.emenu.features.resource.service.impl;

import com.emenu.enums.resource.FileStatus;
import com.emenu.enums.resource.FileType;
import com.emenu.exception.custom.NotFoundException;
import com.emenu.features.appkey.models.AppKey;
import com.emenu.features.appkey.service.AppKeyService;
import com.emenu.features.resource.dto.request.ResourceUploadRequest;
import com.emenu.features.resource.dto.response.ResourceCountResponse;
import com.emenu.features.resource.dto.response.ResourceFileResponse;
import com.emenu.features.resource.kafka.event.ResourceDeleteEvent;
import com.emenu.features.resource.kafka.event.ResourceUploadEvent;
import com.emenu.features.resource.kafka.producer.ResourceFileProducer;
import com.emenu.features.resource.mapper.ResourceFileMapper;
import com.emenu.features.resource.models.ResourceFile;
import com.emenu.features.resource.repository.ResourceFileRepository;
import com.emenu.features.resource.service.ResourceFileService;
import com.emenu.features.resource.service.ResourceTrackerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceFileServiceImpl implements ResourceFileService {

    private final ResourceFileRepository resourceFileRepository;
    private final ResourceFileMapper resourceFileMapper;
    private final AppKeyService appKeyService;
    private final ResourceFileProducer resourceFileProducer;
    private final ResourceTrackerService resourceTrackerService;

    @Value("${resource.storage.base-path:/app/storage}")
    private String storagePath;

    // Folder path keeps yyyy-MM-dd for directory organisation
    private static final DateTimeFormatter FOLDER_DATE   = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // Physical filename prefix: ddMMyyyy  e.g. 06032026
    private static final DateTimeFormatter FILE_DATE     = DateTimeFormatter.ofPattern("ddMMyyyy");

    // ─────────────────────── UPLOAD ───────────────────────────────

    @Override
    @Transactional
    public ResourceFileResponse upload(ResourceUploadRequest request) {
        // 1. Validate API key → get application name
        AppKey appKey = appKeyService.validateAndGetAppKey(request.getKey());
        String appName = appKey.getApplicationName();

        // 2. Detect mimeType from data URI prefix, then strip it
        String mimeType   = mimeTypeFromBase64(request.getBase64());
        String rawBase64  = stripBase64Prefix(request.getBase64());

        // 3. Determine file type and build folder path
        FileType fileType = resolveFileType(mimeType);
        LocalDate today   = LocalDate.now();

        // Folder structure: appName/yyyy-MM-dd/
        String folderPath = appName + "/" + today.format(FOLDER_DATE) + "/";

        // 4. Generate filename: ddMMyyyy_xxxxxxxx.ext  (e.g. 06032026_a1b2c3d4.jpg)
        String extension = extensionFromMime(mimeType);
        String shortId    = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String physicalFileName = today.format(FILE_DATE) + "_" + shortId
                + (extension.isEmpty() ? "" : "." + extension);
        String filePath = folderPath + physicalFileName;

        // 5. Upsert tracker: create or refresh lastUsedAt for this (appName, resourceId) pair
        UUID trackerId = (request.getResourceId() != null && !request.getResourceId().isBlank())
                ? resourceTrackerService.upsert(appName, request.getResourceId())
                : null;

        // 6. Persist metadata record with PENDING status (no file bytes in DB)
        ResourceFile resourceFile = new ResourceFile();
        resourceFile.setFileUuid(physicalFileName);
        resourceFile.setMimeType(mimeType);
        resourceFile.setFileType(fileType);
        resourceFile.setApplicationName(appName);
        resourceFile.setResourceId(request.getResourceId());
        resourceFile.setFilePath(filePath);
        resourceFile.setStatus(FileStatus.PENDING);
        resourceFile.setResourceTrackerId(trackerId);

        ResourceFile saved = resourceFileRepository.save(resourceFile);

        // 7. Send Kafka event for async processing (actual disk write happens in consumer)
        ResourceUploadEvent event = ResourceUploadEvent.builder()
                .resourceFileId(saved.getId().toString())
                .applicationName(appName)
                .resourceId(request.getResourceId())
                .fileUuid(physicalFileName)
                .filePath(filePath)
                .mimeType(mimeType)
                .base64Data(rawBase64)
                .build();

        resourceFileProducer.sendUploadEvent(event);
        log.info("Upload queued for file: {} | app: {} | resourceId: {}",
                physicalFileName, appName, request.getResourceId());

        return resourceFileMapper.toResponse(saved);
    }

    // ─────────────────────── PREVIEW ──────────────────────────────

    @Override
    public byte[] preview(String filePath) {
        ResourceFile resourceFile = resourceFileRepository.findByFilePathAndIsDeletedFalse(filePath)
                .orElseThrow(() -> new NotFoundException("File not found: " + filePath));

        if (resourceFile.getStatus() != FileStatus.COMPLETED) {
            throw new IllegalStateException(
                    "File is not ready yet. Current status: " + resourceFile.getStatus());
        }

        try {
            return Files.readAllBytes(Paths.get(storagePath, filePath));
        } catch (IOException e) {
            log.error("Failed to read file: {} | error: {}", filePath, e.getMessage());
            throw new NotFoundException("File not found on disk: " + filePath);
        }
    }

    // ─────────────────────── READ ─────────────────────────────────

    @Override
    @Cacheable(value = "resource-files", key = "#id")
    public ResourceFileResponse getById(UUID id) {
        return resourceFileMapper.toResponse(findActiveById(id));
    }

    @Override
    public List<ResourceFileResponse> listByResourceId(String resourceId) {
        return resourceFileRepository.findByResourceIdAndIsDeletedFalse(resourceId)
                .stream()
                .map(resourceFileMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────── DELETE ───────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "resource-files", key = "#id")
    public void deleteById(UUID id) {
        ResourceFile resourceFile = findActiveById(id);
        resourceFile.softDelete();
        resourceFileRepository.save(resourceFile);

        // Schedule physical file removal via Kafka
        ResourceDeleteEvent event = ResourceDeleteEvent.builder()
                .filePaths(List.of(resourceFile.getFilePath()))
                .resourceId(resourceFile.getResourceId())
                .applicationName(resourceFile.getApplicationName())
                .build();

        resourceFileProducer.sendDeleteEvent(event);
        log.info("Soft-deleted and queued physical deletion for file: {}", resourceFile.getFilePath());
    }

    @Override
    @Transactional
    public void deleteAllByResourceId(String resourceId) {
        List<ResourceFile> files = resourceFileRepository.findByResourceIdAndIsDeletedFalse(resourceId);

        if (files.isEmpty()) {
            log.info("No active files found for resourceId: {}", resourceId);
            return;
        }

        List<String> filePaths = files.stream()
                .map(ResourceFile::getFilePath)
                .collect(Collectors.toList());

        // Bulk soft-delete all DB records
        files.forEach(ResourceFile::softDelete);
        resourceFileRepository.saveAll(files);

        // Send one delete event with all paths — consumer handles physical removal
        String appName = files.get(0).getApplicationName();
        ResourceDeleteEvent event = ResourceDeleteEvent.builder()
                .filePaths(filePaths)
                .resourceId(resourceId)
                .applicationName(appName)
                .build();

        resourceFileProducer.sendDeleteEvent(event);
        log.info("Bulk soft-deleted {} files for resourceId: {}", files.size(), resourceId);
    }

    @Override
    @Transactional
    public void deleteAllByApplicationName(String applicationName) {
        List<ResourceFile> files = resourceFileRepository.findByApplicationNameAndIsDeletedFalse(applicationName);

        if (files.isEmpty()) {
            log.info("No active files found for applicationName: {}", applicationName);
            return;
        }

        List<String> filePaths = files.stream()
                .map(ResourceFile::getFilePath)
                .collect(Collectors.toList());

        files.forEach(ResourceFile::softDelete);
        resourceFileRepository.saveAll(files);

        ResourceDeleteEvent event = ResourceDeleteEvent.builder()
                .filePaths(filePaths)
                .resourceId("bulk-app-delete")
                .applicationName(applicationName)
                .build();

        resourceFileProducer.sendDeleteEvent(event);
        log.info("Bulk soft-deleted {} files for applicationName: {}", files.size(), applicationName);
    }

    // ─────────────────────── COUNTS ───────────────────────────────

    @Override
    public ResourceCountResponse countByResourceId(String resourceId) {
        long count = resourceFileRepository.countByResourceId(resourceId);
        return ResourceCountResponse.builder()
                .resourceId(resourceId)
                .totalFiles(count)
                .build();
    }

    @Override
    public ResourceCountResponse countByApplicationName(String applicationName) {
        long count = resourceFileRepository.countByApplicationName(applicationName);
        return ResourceCountResponse.builder()
                .applicationName(applicationName)
                .totalFiles(count)
                .build();
    }

    // ─────────────────────── MULTIPART UPLOAD ─────────────────────

    @Override
    @Transactional
    public ResourceFileResponse uploadMultipart(String key, String resourceId,
                                                org.springframework.web.multipart.MultipartFile file) {
        AppKey appKey = appKeyService.validateAndGetAppKey(key);
        String appName = appKey.getApplicationName();

        String mimeType   = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        FileType fileType = resolveFileType(mimeType);
        LocalDate today   = LocalDate.now();
        // Folder structure: appName/yyyy-MM-dd/
        String folderPath = appName + "/" + today.format(FOLDER_DATE) + "/";

        String extension      = extensionFromMime(mimeType);
        String shortId        = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String physicalFileName = today.format(FILE_DATE) + "_" + shortId
                + (extension.isEmpty() ? "" : "." + extension);
        String filePath = folderPath + physicalFileName;

        // Upsert tracker: create or refresh lastUsedAt for this (appName, resourceId) pair
        UUID trackerId = (resourceId != null && !resourceId.isBlank())
                ? resourceTrackerService.upsert(appName, resourceId)
                : null;

        // Convert multipart bytes to base64 for Kafka event (consumer will write to disk)
        String base64Data;
        try {
            base64Data = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            log.error("Failed to read multipart file bytes: {}", e.getMessage());
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage());
        }

        // Persist metadata with PENDING status — actual disk write happens in Kafka consumer
        ResourceFile resourceFile = new ResourceFile();
        resourceFile.setFileUuid(physicalFileName);
        resourceFile.setMimeType(mimeType);
        resourceFile.setFileType(fileType);
        resourceFile.setApplicationName(appName);
        resourceFile.setResourceId(resourceId);
        resourceFile.setFilePath(filePath);
        resourceFile.setResourceTrackerId(trackerId);
        resourceFile.setStatus(FileStatus.PENDING);

        ResourceFile saved = resourceFileRepository.save(resourceFile);

        // Send Kafka event — consumer decodes base64 and writes file to disk
        ResourceUploadEvent event = ResourceUploadEvent.builder()
                .resourceFileId(saved.getId().toString())
                .applicationName(appName)
                .resourceId(resourceId)
                .fileUuid(physicalFileName)
                .filePath(filePath)
                .mimeType(mimeType)
                .base64Data(base64Data)
                .build();

        resourceFileProducer.sendUploadEvent(event);
        log.info("Multipart upload queued via Kafka: {} | app: {} | resourceId: {}", physicalFileName, appName, resourceId);
        return resourceFileMapper.toResponse(saved);
    }

    // ─────────────────────── HELPERS ──────────────────────────────

    private ResourceFile findActiveById(UUID id) {
        return resourceFileRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Resource file not found with id: " + id));
    }

    private FileType resolveFileType(String mimeType) {
        if (mimeType != null && mimeType.startsWith("image/")) {
            return FileType.IMAGE;
        }
        return FileType.DOCUMENT;
    }

    /** Derive file extension from MIME type (e.g. image/jpeg → jpg). */
    private String extensionFromMime(String mimeType) {
        if (mimeType == null) return "";
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg"                -> "jpg";
            case "image/png"                 -> "png";
            case "image/gif"                 -> "gif";
            case "image/webp"                -> "webp";
            case "image/svg+xml"             -> "svg";
            case "application/pdf"           -> "pdf";
            case "application/msword"        -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.ms-excel"  -> "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"       -> "xlsx";
            case "text/plain"                -> "txt";
            case "video/mp4"                 -> "mp4";
            default -> "";
        };
    }

    private String mimeTypeFromBase64(String base64) {
        if (base64 != null && base64.startsWith("data:") && base64.contains(";base64,")) {
            return base64.substring(5, base64.indexOf(';'));
        }
        return "application/octet-stream";
    }

    private String stripBase64Prefix(String base64) {
        if (base64 != null && base64.contains(",")) {
            return base64.substring(base64.indexOf(',') + 1);
        }
        return base64;
    }
}
