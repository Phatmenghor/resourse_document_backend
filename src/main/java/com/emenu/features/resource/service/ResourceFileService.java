package com.emenu.features.resource.service;

import com.emenu.features.resource.dto.request.ResourceUploadRequest;
import com.emenu.features.resource.dto.response.ResourceCountResponse;
import com.emenu.features.resource.dto.response.ResourceFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ResourceFileService {

    /**
     * Base64 upload — validates API key, persists metadata PENDING, dispatches Kafka for async write.
     */
    ResourceFileResponse upload(ResourceUploadRequest request);

    /**
     * Multipart upload — validates API key, converts bytes to base64, persists metadata PENDING,
     * and dispatches Kafka event for async disk write (same pipeline as base64 upload).
     */
    ResourceFileResponse uploadMultipart(String key, String resourceId, MultipartFile file);

    /**
     * Stream the file bytes back for preview/download.
     */
    byte[] preview(UUID id);

    /**
     * Get metadata for a single file.
     */
    ResourceFileResponse getById(UUID id);

    /**
     * List all files for a given resourceId (business ID).
     */
    List<ResourceFileResponse> listByResourceId(String resourceId);

    /**
     * Delete a single file by ID (soft-delete metadata + schedule physical file removal via Kafka).
     */
    void deleteById(UUID id);

    /**
     * Bulk-delete all files that belong to a resourceId (e.g. when a business stops service).
     * Soft-deletes all metadata records and schedules physical removal via Kafka.
     */
    void deleteAllByResourceId(String resourceId);

    /**
     * Bulk-delete all files that belong to an applicationName (e.g. when an API key is revoked).
     * Soft-deletes all metadata records and schedules physical removal via Kafka.
     */
    void deleteAllByApplicationName(String applicationName);

    /**
     * Count files – useful for monitoring storage quotas.
     */
    ResourceCountResponse countByResourceId(String resourceId);

    ResourceCountResponse countByApplicationName(String applicationName);
}
