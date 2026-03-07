package com.emenu.features.resource.service;

import com.emenu.features.resource.dto.request.ResourceUploadBatchRequest;
import com.emenu.features.resource.dto.request.ResourceUploadRequest;
import com.emenu.features.resource.dto.response.ResourceFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
     * Batch base64 upload — uploads multiple files in one request, all sharing the same key and resourceId.
     */
    List<ResourceFileResponse> uploadBatch(ResourceUploadBatchRequest request);

    /**
     * Batch multipart upload — uploads multiple files in one request, all sharing the same key and resourceId.
     */
    List<ResourceFileResponse> uploadMultipartBatch(String key, String resourceId, List<MultipartFile> files);

    /**
     * Stream the file bytes back for preview/download by relative file path.
     */
    byte[] preview(String filePath);

    /**
     * Delete a single file by filename (e.g. 07032026_d093324b.jpg).
     */
    void deleteByFilename(String filename);

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

}
