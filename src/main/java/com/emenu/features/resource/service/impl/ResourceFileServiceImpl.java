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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    @Value("${resource.storage.base-path:/app/storage}")
    private String storagePath;

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─────────────────────── UPLOAD ───────────────────────────────

    @Override
    @Transactional
    public ResourceFileResponse upload(ResourceUploadRequest request) {
        // 1. Validate API key → get application name
        AppKey appKey = appKeyService.validateAndGetAppKey(request.getKey());
        String appName = appKey.getApplicationName();

        // 2. Strip data URI prefix from base64 if present (e.g. "data:image/jpeg;base64,...")
        String rawBase64 = stripBase64Prefix(request.getBase64());

        // 3. Determine file type and build folder path
        FileType fileType = resolveFileType(request.getMimeType());
        String today = LocalDate.now().format(DAY_FORMATTER);
        String subFolder = fileType == FileType.IMAGE ? "images" : "documents";

        // Folder structure: applicationName/yyyy-MM-dd/images|documents/
        String folderPath = appName + "/" + today + "/" + subFolder + "/";

        // 4. Generate a UUID-based filename
        String extension = extractExtension(request.getFileName());
        String fileUuid = UUID.randomUUID().toString();
        String physicalFileName = fileUuid + (extension.isEmpty() ? "" : "." + extension);
        String filePath = folderPath + physicalFileName;

        // 5. Persist metadata record with PENDING status (no file bytes in DB)
        ResourceFile resourceFile = new ResourceFile();
        resourceFile.setFileUuid(physicalFileName);
        resourceFile.setOriginalFileName(request.getFileName());
        resourceFile.setMimeType(request.getMimeType());
        resourceFile.setFileType(fileType);
        resourceFile.setApplicationName(appName);
        resourceFile.setResourceId(request.getResourceId());
        resourceFile.setUploadDay(today);
        resourceFile.setFolderPath(folderPath);
        resourceFile.setFilePath(filePath);
        resourceFile.setStatus(FileStatus.PENDING);

        ResourceFile saved = resourceFileRepository.save(resourceFile);

        // 6. Send Kafka event for async processing (actual disk write happens in consumer)
        ResourceUploadEvent event = ResourceUploadEvent.builder()
                .resourceFileId(saved.getId().toString())
                .applicationName(appName)
                .resourceId(request.getResourceId())
                .fileUuid(physicalFileName)
                .folderPath(folderPath)
                .filePath(filePath)
                .mimeType(request.getMimeType())
                .originalFileName(request.getFileName())
                .base64Data(rawBase64)
                .build();

        resourceFileProducer.sendUploadEvent(event);
        log.info("Upload queued for file: {} | app: {} | resourceId: {}",
                physicalFileName, appName, request.getResourceId());

        return resourceFileMapper.toResponse(saved);
    }

    // ─────────────────────── PREVIEW ──────────────────────────────

    @Override
    public byte[] preview(UUID id) {
        ResourceFile resourceFile = findActiveById(id);

        if (resourceFile.getStatus() != FileStatus.COMPLETED) {
            throw new IllegalStateException(
                    "File is not ready yet. Current status: " + resourceFile.getStatus());
        }

        try {
            return Files.readAllBytes(Paths.get(storagePath, resourceFile.getFilePath()));
        } catch (IOException e) {
            log.error("Failed to read file: {} | error: {}", resourceFile.getFilePath(), e.getMessage());
            throw new NotFoundException("File not found on disk: " + resourceFile.getFilePath());
        }
    }

    // ─────────────────────── READ ─────────────────────────────────

    @Override
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

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String stripBase64Prefix(String base64) {
        if (base64 != null && base64.contains(",")) {
            return base64.substring(base64.indexOf(',') + 1);
        }
        return base64;
    }
}
