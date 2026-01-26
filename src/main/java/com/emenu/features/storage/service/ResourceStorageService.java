package com.emenu.features.storage.service;

import com.emenu.features.storage.dto.filter.ResourceFilterRequest;
import com.emenu.features.storage.dto.filter.StorageLogFilterRequest;
import com.emenu.features.storage.dto.request.UploadResourceRequest;
import com.emenu.features.storage.dto.response.*;
import com.emenu.features.storage.models.StorageApiKey;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface ResourceStorageService {

    /**
     * Upload a resource file
     */
    ResourceUploadResponse uploadResource(String apiKey, UploadResourceRequest request, HttpServletRequest httpRequest);

    /**
     * Download a resource file
     */
    ResourceDownloadResponse downloadResource(String apiKey, UUID resourceId, HttpServletRequest httpRequest);

    /**
     * Get resource metadata (without downloading)
     */
    ResourceFileResponse getResourceMetadata(String apiKey, UUID resourceId);

    /**
     * List resources for an API key
     */
    PaginationResponse<ResourceFileResponse> listResources(String apiKey, ResourceFilterRequest request);

    /**
     * Delete a resource
     */
    void deleteResource(String apiKey, UUID resourceId, HttpServletRequest httpRequest);

    /**
     * Get storage logs for an API key
     */
    PaginationResponse<ResourceStorageLogResponse> getStorageLogs(String apiKey, StorageLogFilterRequest request);

    /**
     * Get storage statistics for an API key
     */
    StorageStatsResponse getStorageStats(String apiKey);

    /**
     * Clean up expired temporary files
     */
    int cleanupExpiredFiles();

    /**
     * Public download endpoint (for temporary URLs)
     */
    ResourceDownloadResponse publicDownload(UUID resourceId);
}
