package com.emenu.features.storage.service.impl;

import com.emenu.enums.storage.ResourceType;
import com.emenu.enums.storage.StorageLogAction;
import com.emenu.enums.storage.StorageStatus;
import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.storage.dto.filter.ResourceFilterRequest;
import com.emenu.features.storage.dto.filter.StorageLogFilterRequest;
import com.emenu.features.storage.dto.request.UploadResourceRequest;
import com.emenu.features.storage.dto.response.*;
import com.emenu.features.storage.mapper.ResourceFileMapper;
import com.emenu.features.storage.mapper.ResourceStorageLogMapper;
import com.emenu.features.storage.models.ResourceFile;
import com.emenu.features.storage.models.ResourceStorageLog;
import com.emenu.features.storage.models.StorageApiKey;
import com.emenu.features.storage.repository.ResourceFileRepository;
import com.emenu.features.storage.repository.ResourceStorageLogRepository;
import com.emenu.features.storage.service.ResourceStorageService;
import com.emenu.features.storage.service.StorageApiKeyService;
import com.emenu.features.storage.util.EncryptionUtil;
import com.emenu.features.storage.util.StorageUtils;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.pagination.PaginationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResourceStorageServiceImpl implements ResourceStorageService {

    private final ResourceFileRepository resourceFileRepository;
    private final ResourceStorageLogRepository storageLogRepository;
    private final StorageApiKeyService apiKeyService;
    private final ResourceFileMapper resourceFileMapper;
    private final ResourceStorageLogMapper storageLogMapper;
    private final EncryptionUtil encryptionUtil;

    @Override
    public ResourceUploadResponse uploadResource(String apiKeyValue, UploadResourceRequest request, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        StorageApiKey apiKey = apiKeyService.validateAndGetApiKey(apiKeyValue);

        log.info("Uploading resource for API key: {}", apiKey.getName());

        try {
            // Decode and validate the data
            byte[] fileData;
            try {
                fileData = Base64.getDecoder().decode(request.getData());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid base64 encoded data");
            }

            long fileSizeBytes = fileData.length;

            // Validate file size
            if (!StorageUtils.isFileSizeValid(fileSizeBytes, apiKey.getMaxFileSizeMb())) {
                throw new ValidationException("File size exceeds maximum allowed size of " + apiKey.getMaxFileSizeMb() + "MB");
            }

            // Check storage quota
            long maxStorageBytes = apiKey.getMaxStorageMb() * 1024 * 1024;
            if (apiKey.getCurrentStorageBytes() + fileSizeBytes > maxStorageBytes) {
                throw new ValidationException("Storage quota exceeded. Current: " +
                        StorageUtils.formatBytes(apiKey.getCurrentStorageBytes()) +
                        ", Max: " + StorageUtils.formatBytes(maxStorageBytes));
            }

            // Determine resource type
            ResourceType resourceType = StorageUtils.getResourceType(request.getContentType());

            // Validate file type
            if (!StorageUtils.isFileTypeAllowed(resourceType, apiKey.getAllowedFileTypes())) {
                throw new ValidationException("File type " + resourceType + " is not allowed for this API key");
            }

            // Generate checksum
            String checksum = encryptionUtil.generateChecksum(fileData);

            // Encrypt the data
            EncryptionUtil.EncryptionResult encryptionResult = encryptionUtil.encrypt(request.getData());

            // Generate stored filename and path
            String storedFilename = StorageUtils.generateStoredFilename(request.getFilename());
            String storagePath = StorageUtils.buildStoragePath(apiKey.getStoragePath(), storedFilename);

            // Create resource file entity
            ResourceFile resourceFile = new ResourceFile();
            resourceFile.setApiKey(apiKey);
            resourceFile.setOriginalFilename(request.getFilename());
            resourceFile.setStoredFilename(storedFilename);
            resourceFile.setStoragePath(storagePath);
            resourceFile.setResourceType(resourceType);
            resourceFile.setMimeType(request.getContentType());
            resourceFile.setFileSizeBytes(fileSizeBytes);
            resourceFile.setEncryptedData(encryptionResult.getEncryptedData());
            resourceFile.setEncryptionIv(encryptionResult.getIv());
            resourceFile.setIsEncrypted(true);
            resourceFile.setStatus(StorageStatus.ACTIVE);
            resourceFile.setChecksum(checksum);
            resourceFile.setDownloadCount(0L);
            resourceFile.setMetadata(request.getMetadata());

            // Handle temporary file settings
            if (Boolean.TRUE.equals(request.getIsTemporary())) {
                resourceFile.setIsTemporary(true);
                long expiresInMinutes = request.getExpiresInMinutes() != null ? request.getExpiresInMinutes() : 60; // Default 1 hour
                resourceFile.setExpiresAt(LocalDateTime.now().plusMinutes(expiresInMinutes));
            } else {
                resourceFile.setIsTemporary(false);
            }

            // Save resource file
            ResourceFile saved = resourceFileRepository.save(resourceFile);

            // Update API key statistics
            apiKey.incrementUploadCount();
            apiKey.addStorageBytes(fileSizeBytes);

            // Log the upload
            long duration = System.currentTimeMillis() - startTime;
            createLog(apiKey, saved.getId(), request.getFilename(), StorageLogAction.UPLOAD,
                    fileSizeBytes, true, null, httpRequest, duration);

            log.info("Resource uploaded successfully: {} ({})", saved.getId(), storedFilename);
            return resourceFileMapper.toUploadResponse(saved);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            createLog(apiKey, null, request.getFilename(), StorageLogAction.UPLOAD,
                    null, false, e.getMessage(), httpRequest, duration);
            throw e;
        }
    }

    @Override
    public ResourceDownloadResponse downloadResource(String apiKeyValue, UUID resourceId, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        StorageApiKey apiKey = apiKeyService.validateAndGetApiKey(apiKeyValue);

        log.info("Downloading resource: {} for API key: {}", resourceId, apiKey.getName());

        try {
            ResourceFile resourceFile = findResourceFile(resourceId);

            // Verify resource belongs to this API key
            if (!resourceFile.getApiKey().getId().equals(apiKey.getId())) {
                throw new ValidationException("Resource does not belong to this API key");
            }

            // Check if expired
            if (resourceFile.isExpired()) {
                throw new ValidationException("Resource has expired");
            }

            // Check if active
            if (resourceFile.getStatus() != StorageStatus.ACTIVE) {
                throw new ValidationException("Resource is not available (status: " + resourceFile.getStatus() + ")");
            }

            // Decrypt the data
            String decryptedData = encryptionUtil.decrypt(
                    resourceFile.getEncryptedData(),
                    resourceFile.getEncryptionIv()
            );

            byte[] fileData = Base64.getDecoder().decode(decryptedData);

            // Update statistics
            resourceFile.incrementDownloadCount();
            apiKey.incrementDownloadCount();

            // Log the download
            long duration = System.currentTimeMillis() - startTime;
            createLog(apiKey, resourceId, resourceFile.getOriginalFilename(), StorageLogAction.DOWNLOAD,
                    resourceFile.getFileSizeBytes(), true, null, httpRequest, duration);

            log.info("Resource downloaded successfully: {}", resourceId);

            return ResourceDownloadResponse.builder()
                    .filename(resourceFile.getOriginalFilename())
                    .mimeType(resourceFile.getMimeType())
                    .data(fileData)
                    .fileSizeBytes(resourceFile.getFileSizeBytes())
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            createLog(apiKey, resourceId, null, StorageLogAction.DOWNLOAD,
                    null, false, e.getMessage(), httpRequest, duration);
            throw e;
        }
    }

    @Override
    public ResourceFileResponse getResourceMetadata(String apiKeyValue, UUID resourceId) {
        StorageApiKey apiKey = apiKeyService.validateAndGetApiKey(apiKeyValue);
        ResourceFile resourceFile = findResourceFile(resourceId);

        // Verify resource belongs to this API key
        if (!resourceFile.getApiKey().getId().equals(apiKey.getId())) {
            throw new ValidationException("Resource does not belong to this API key");
        }

        return resourceFileMapper.toResponse(resourceFile);
    }

    @Override
    public PaginationResponse<ResourceFileResponse> listResources(String apiKeyValue, ResourceFilterRequest request) {
        StorageApiKey apiKey = apiKeyService.validateAndGetApiKey(apiKeyValue);

        Pageable pageable = PaginationUtils.createPageable(
                request.getPageNo(),
                request.getPageSize(),
                request.getSortBy(),
                request.getSortDirection()
        );

        Page<ResourceFile> page = resourceFileRepository.searchResources(
                apiKey.getId(),
                request.getResourceType(),
                request.getStatus(),
                request.getFilename(),
                pageable
        );

        return resourceFileMapper.toPaginationResponse(page);
    }

    @Override
    public void deleteResource(String apiKeyValue, UUID resourceId, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        StorageApiKey apiKey = apiKeyService.validateAndGetApiKey(apiKeyValue);

        log.info("Deleting resource: {} for API key: {}", resourceId, apiKey.getName());

        try {
            ResourceFile resourceFile = findResourceFile(resourceId);

            // Verify resource belongs to this API key
            if (!resourceFile.getApiKey().getId().equals(apiKey.getId())) {
                throw new ValidationException("Resource does not belong to this API key");
            }

            // Update storage statistics
            apiKey.subtractStorageBytes(resourceFile.getFileSizeBytes());

            // Soft delete
            resourceFile.setStatus(StorageStatus.DELETED);
            resourceFile.softDelete("API_KEY:" + apiKey.getName());

            // Log the deletion
            long duration = System.currentTimeMillis() - startTime;
            createLog(apiKey, resourceId, resourceFile.getOriginalFilename(), StorageLogAction.DELETE,
                    resourceFile.getFileSizeBytes(), true, null, httpRequest, duration);

            log.info("Resource deleted successfully: {}", resourceId);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            createLog(apiKey, resourceId, null, StorageLogAction.DELETE,
                    null, false, e.getMessage(), httpRequest, duration);
            throw e;
        }
    }

    @Override
    public PaginationResponse<ResourceStorageLogResponse> getStorageLogs(String apiKeyValue, StorageLogFilterRequest request) {
        StorageApiKey apiKey = apiKeyService.validateAndGetApiKey(apiKeyValue);

        Pageable pageable = PaginationUtils.createPageable(
                request.getPageNo(),
                request.getPageSize(),
                request.getSortBy(),
                request.getSortDirection()
        );

        Page<ResourceStorageLog> page = storageLogRepository.searchLogs(
                apiKey.getId(),
                request.getAction(),
                request.getIsSuccess(),
                request.getStartDate(),
                request.getEndDate(),
                pageable
        );

        return storageLogMapper.toPaginationResponse(page);
    }

    @Override
    public StorageStatsResponse getStorageStats(String apiKeyValue) {
        StorageApiKey apiKey = apiKeyService.validateAndGetApiKey(apiKeyValue);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        Long totalFiles = resourceFileRepository.countActiveFilesByApiKey(apiKey.getId());
        Long totalStorageBytes = resourceFileRepository.sumFileSizeByApiKey(apiKey.getId());
        Long maxStorageBytes = apiKey.getMaxStorageMb() * 1024 * 1024;

        Long uploadsToday = storageLogRepository.countSuccessfulActionsBetween(
                apiKey.getId(), StorageLogAction.UPLOAD, startOfDay, endOfDay);
        Long downloadsToday = storageLogRepository.countSuccessfulActionsBetween(
                apiKey.getId(), StorageLogAction.DOWNLOAD, startOfDay, endOfDay);
        Long uploadBytesToday = storageLogRepository.sumUploadedBytesBetween(
                apiKey.getId(), startOfDay, endOfDay);

        double usedPercentage = maxStorageBytes > 0
                ? Math.round((totalStorageBytes * 100.0 / maxStorageBytes) * 100.0) / 100.0
                : 0.0;

        return StorageStatsResponse.builder()
                .apiKeyId(apiKey.getId())
                .apiKeyName(apiKey.getName())
                .totalFiles(totalFiles)
                .totalStorageBytes(totalStorageBytes)
                .totalStorageFormatted(StorageUtils.formatBytes(totalStorageBytes))
                .maxStorageBytes(maxStorageBytes)
                .maxStorageFormatted(StorageUtils.formatBytes(maxStorageBytes))
                .usedPercentage(usedPercentage)
                .totalUploads(apiKey.getTotalUploadCount())
                .totalDownloads(apiKey.getTotalDownloadCount())
                .uploadsToday(uploadsToday)
                .downloadsToday(downloadsToday)
                .uploadBytesToday(uploadBytesToday)
                .uploadBytesTodayFormatted(StorageUtils.formatBytes(uploadBytesToday != null ? uploadBytesToday : 0L))
                .build();
    }

    @Override
    public int cleanupExpiredFiles() {
        log.info("Starting cleanup of expired temporary files");

        List<ResourceFile> expiredFiles = resourceFileRepository.findExpiredTemporaryFiles(LocalDateTime.now());

        int count = 0;
        for (ResourceFile file : expiredFiles) {
            try {
                // Update storage statistics
                file.getApiKey().subtractStorageBytes(file.getFileSizeBytes());

                // Mark as expired
                file.setStatus(StorageStatus.EXPIRED);
                file.softDelete("SYSTEM_CLEANUP");
                resourceFileRepository.save(file);

                // Log the cleanup
                ResourceStorageLog log = ResourceStorageLog.createLog(file.getApiKey(), StorageLogAction.EXPIRE_CLEANUP);
                log.setResourceFileId(file.getId());
                log.setOriginalFilename(file.getOriginalFilename());
                log.setFileSizeBytes(file.getFileSizeBytes());
                storageLogRepository.save(log);

                count++;
            } catch (Exception e) {
                log.error("Error cleaning up expired file {}: {}", file.getId(), e.getMessage());
            }
        }

        log.info("Cleanup completed. {} expired files processed", count);
        return count;
    }

    @Override
    public ResourceDownloadResponse publicDownload(UUID resourceId) {
        log.info("Public download for resource: {}", resourceId);

        ResourceFile resourceFile = findResourceFile(resourceId);

        // Check if expired
        if (resourceFile.isExpired()) {
            throw new ValidationException("Resource has expired");
        }

        // Check if active
        if (resourceFile.getStatus() != StorageStatus.ACTIVE) {
            throw new ValidationException("Resource is not available");
        }

        // Decrypt the data
        String decryptedData = encryptionUtil.decrypt(
                resourceFile.getEncryptedData(),
                resourceFile.getEncryptionIv()
        );

        byte[] fileData = Base64.getDecoder().decode(decryptedData);

        // Update download count
        resourceFile.incrementDownloadCount();
        resourceFile.getApiKey().incrementDownloadCount();

        return ResourceDownloadResponse.builder()
                .filename(resourceFile.getOriginalFilename())
                .mimeType(resourceFile.getMimeType())
                .data(fileData)
                .fileSizeBytes(resourceFile.getFileSizeBytes())
                .build();
    }

    private ResourceFile findResourceFile(UUID resourceId) {
        return resourceFileRepository.findByIdAndIsDeletedFalse(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found with ID: " + resourceId));
    }

    private void createLog(StorageApiKey apiKey, UUID resourceFileId, String filename,
                           StorageLogAction action, Long fileSizeBytes, boolean isSuccess,
                           String errorMessage, HttpServletRequest request, Long durationMs) {
        ResourceStorageLog log = new ResourceStorageLog();
        log.setApiKey(apiKey);
        log.setResourceFileId(resourceFileId);
        log.setOriginalFilename(filename);
        log.setAction(action);
        log.setActionTimestamp(LocalDateTime.now());
        log.setFileSizeBytes(fileSizeBytes);
        log.setIsSuccess(isSuccess);
        log.setErrorMessage(errorMessage);
        log.setDurationMs(durationMs);

        if (request != null) {
            log.setClientIp(getClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setRequestId(UUID.randomUUID().toString());
        }

        storageLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
