package com.emenu.features.resource.service.impl;

import com.emenu.enums.resource.FileStatus;
import com.emenu.exception.custom.NotFoundException;
import com.emenu.features.appkey.models.AppKey;
import com.emenu.features.appkey.service.AppKeyService;
import com.emenu.features.resource.dto.request.DeleteBulkRequest;
import com.emenu.features.resource.dto.request.ResourceFileFilterRequest;
import com.emenu.features.resource.dto.request.ResourceUploadBatchRequest;
import com.emenu.features.resource.dto.request.ResourceUploadRequest;
import com.emenu.features.resource.dto.response.ResourceFileResponse;
import com.emenu.features.resource.kafka.event.ResourceDeleteEvent;
import com.emenu.features.resource.kafka.event.ResourceUploadEvent;
import com.emenu.features.resource.kafka.producer.ResourceFileProducer;
import com.emenu.features.resource.mapper.ResourceFileMapper;
import com.emenu.features.resource.models.ResourceFile;
import com.emenu.features.resource.repository.ResourceFileRepository;
import com.emenu.features.resource.service.ResourceFileService;
import com.emenu.features.resource.service.ResourceTrackerService;
import com.emenu.features.resource.utils.FileUtils;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.pagination.PaginationUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
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
    private final ResourceFileMapper     resourceFileMapper;
    private final AppKeyService          appKeyService;
    private final ResourceFileProducer   resourceFileProducer;
    private final ResourceTrackerService resourceTrackerService;

    @Value("${resource.storage.base-path:/app/storage}")
    private String storagePath;

    private static final DateTimeFormatter FOLDER_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_DATE   = DateTimeFormatter.ofPattern("ddMMyyyy");

    // ─────────────────────── SEARCH ───────────────────────────────

    @Override
    public PaginationResponse<ResourceFileResponse> search(ResourceFileFilterRequest request) {
        Pageable pageable = PaginationUtils.createPageable(
                request.getPageNo(), request.getPageSize(),
                request.getSortBy(), request.getSortDirection());

        String search = request.getSearch() == null ? "" : request.getSearch().trim();

        Page<ResourceFile> page = resourceFileRepository.search(
                search,
                request.getApplicationName(),
                request.getResourceId(),
                request.getFileType(),
                request.getStatus(),
                pageable);

        return PaginationResponse.<ResourceFileResponse>builder()
                .content(page.getContent().stream().map(resourceFileMapper::toResponse).toList())
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    // ─────────────────────── UPLOAD ───────────────────────────────

    @Override
    @Transactional
    public ResourceFileResponse upload(ResourceUploadRequest request) {
        AppKey appKey  = appKeyService.validateAndGetAppKey(request.getKey());
        String appName = appKey.getApplicationName();

        String mimeType  = FileUtils.mimeTypeFromBase64(request.getBase64());
        String rawBase64 = FileUtils.stripBase64Prefix(request.getBase64());

        LocalDate today   = LocalDate.now();
        String folderPath = appName + "/" + today.format(FOLDER_DATE) + "/";
        String extension  = FileUtils.extensionFromMime(mimeType);
        String shortId    = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String filename   = today.format(FILE_DATE) + "_" + shortId + (extension.isEmpty() ? "" : "." + extension);
        String filePath   = folderPath + filename;

        UUID trackerId = resolveTrackerId(appName, request.getResourceId());
        ResourceFile saved = resourceFileRepository.save(
                buildResourceFile(filename, mimeType, appName, request.getResourceId(), filePath, trackerId));

        resourceFileProducer.sendUploadEvent(ResourceUploadEvent.builder()
                .resourceFileId(saved.getId().toString())
                .applicationName(appName)
                .resourceId(request.getResourceId())
                .fileUuid(filename)
                .filePath(filePath)
                .mimeType(mimeType)
                .base64Data(rawBase64)
                .build());

        log.info("Upload queued: {} | app: {} | resourceId: {}", filename, appName, request.getResourceId());
        return resourceFileMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ResourceFileResponse uploadMultipart(String key, String resourceId, MultipartFile file) {
        AppKey appKey  = appKeyService.validateAndGetAppKey(key);
        String appName = appKey.getApplicationName();

        String mimeType   = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        LocalDate today   = LocalDate.now();
        String folderPath = appName + "/" + today.format(FOLDER_DATE) + "/";
        String extension  = FileUtils.extensionFromMime(mimeType);
        String shortId    = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String filename   = today.format(FILE_DATE) + "_" + shortId + (extension.isEmpty() ? "" : "." + extension);
        String filePath   = folderPath + filename;

        String base64Data = encodeToBase64(file);
        UUID trackerId    = resolveTrackerId(appName, resourceId);
        ResourceFile saved = resourceFileRepository.save(
                buildResourceFile(filename, mimeType, appName, resourceId, filePath, trackerId));

        resourceFileProducer.sendUploadEvent(ResourceUploadEvent.builder()
                .resourceFileId(saved.getId().toString())
                .applicationName(appName)
                .resourceId(resourceId)
                .fileUuid(filename)
                .filePath(filePath)
                .mimeType(mimeType)
                .base64Data(base64Data)
                .build());

        log.info("Multipart upload queued: {} | app: {} | resourceId: {}", filename, appName, resourceId);
        return resourceFileMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public List<ResourceFileResponse> uploadBatch(ResourceUploadBatchRequest request) {
        return request.getFiles().stream()
                .map(base64 -> {
                    ResourceUploadRequest single = new ResourceUploadRequest();
                    single.setKey(request.getKey());
                    single.setResourceId(request.getResourceId());
                    single.setBase64(base64);
                    return upload(single);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ResourceFileResponse> uploadMultipartBatch(String key, String resourceId, List<MultipartFile> files) {
        return files.stream()
                .map(file -> uploadMultipart(key, resourceId, file))
                .collect(Collectors.toList());
    }

    // ─────────────────────── PREVIEW ──────────────────────────────

    @Override
    public byte[] preview(String filePath) {
        ResourceFile resourceFile = resourceFileRepository.findByFilePathAndIsDeletedFalse(filePath)
                .orElseThrow(() -> new NotFoundException("File not found: " + filePath));

        if (resourceFile.getStatus() != FileStatus.COMPLETED) {
            throw new IllegalStateException("File is not ready yet. Status: " + resourceFile.getStatus());
        }

        try {
            return Files.readAllBytes(Paths.get(storagePath, filePath));
        } catch (IOException e) {
            log.error("Failed to read file: {} | error: {}", filePath, e.getMessage());
            throw new NotFoundException("File not found on disk: " + filePath);
        }
    }

    // ─────────────────────── DELETE ───────────────────────────────

    @Override
    @Transactional
    public void deleteByFilePath(String filePath) {
        ResourceFile resourceFile = resourceFileRepository.findByFilePathAndIsDeletedFalse(filePath)
                .orElseThrow(() -> new NotFoundException("File not found: " + filePath));
        softDeleteAndPublish(List.of(resourceFile), resourceFile.getResourceId(), resourceFile.getApplicationName());
        log.info("Deleted file: {}", filePath);
    }

    @Override
    @Transactional
    public void deleteAllByResourceId(String resourceId) {
        List<ResourceFile> files = resourceFileRepository.findByResourceIdAndIsDeletedFalse(resourceId);
        if (files.isEmpty()) { log.info("No active files for resourceId: {}", resourceId); return; }
        softDeleteAndPublish(files, resourceId, files.get(0).getApplicationName());
        log.info("Bulk deleted {} files for resourceId: {}", files.size(), resourceId);
    }

    @Override
    @Transactional
    public void deleteAllByApplicationName(String applicationName) {
        List<ResourceFile> files = resourceFileRepository.findByApplicationNameAndIsDeletedFalse(applicationName);
        if (files.isEmpty()) { log.info("No active files for applicationName: {}", applicationName); return; }
        softDeleteAndPublish(files, "bulk-app-delete", applicationName);
        log.info("Bulk deleted {} files for applicationName: {}", files.size(), applicationName);
    }

    @Override
    @Transactional
    public void deleteBulk(DeleteBulkRequest request) {
        if (hasText(request.getApiKey())) {
            AppKey appKey = appKeyService.validateAndGetAppKey(request.getApiKey());
            deleteAllByApplicationName(appKey.getApplicationName());
        } else if (hasText(request.getApplicationName())) {
            deleteAllByApplicationName(request.getApplicationName());
        } else if (hasText(request.getResourceId())) {
            deleteAllByResourceId(request.getResourceId());
        } else {
            throw new IllegalArgumentException("Provide one of: resourceId, applicationName, or apiKey");
        }
    }

    // ─────────────────────── PRIVATE HELPERS ──────────────────────

    private ResourceFile buildResourceFile(String filename, String mimeType, String appName,
                                           String resourceId, String filePath, UUID trackerId) {
        ResourceFile rf = new ResourceFile();
        rf.setFileUuid(filename);
        rf.setMimeType(mimeType);
        rf.setFileType(FileUtils.resolveFileType(mimeType));
        rf.setApplicationName(appName);
        rf.setResourceId(resourceId);
        rf.setFilePath(filePath);
        rf.setStatus(FileStatus.PENDING);
        rf.setResourceTrackerId(trackerId);
        return rf;
    }

    private void softDeleteAndPublish(List<ResourceFile> files, String resourceId, String applicationName) {
        files.forEach(ResourceFile::softDelete);
        resourceFileRepository.saveAll(files);
        List<String> paths = files.stream().map(ResourceFile::getFilePath).collect(Collectors.toList());
        resourceFileProducer.sendDeleteEvent(ResourceDeleteEvent.builder()
                .filePaths(paths)
                .resourceId(resourceId)
                .applicationName(applicationName)
                .build());
    }

    private UUID resolveTrackerId(String appName, String resourceId) {
        return hasText(resourceId) ? resourceTrackerService.upsert(appName, resourceId) : null;
    }

    private String encodeToBase64(MultipartFile file) {
        try {
            return Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            log.error("Failed to read multipart file bytes: {}", e.getMessage());
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
