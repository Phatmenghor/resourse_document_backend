package com.emenu.features.storage.service;

import com.emenu.features.storage.dto.filter.ApiKeyFilterRequest;
import com.emenu.features.storage.dto.request.CreateStorageApiKeyRequest;
import com.emenu.features.storage.dto.request.UpdateStorageApiKeyRequest;
import com.emenu.features.storage.dto.response.StorageApiKeyCreatedResponse;
import com.emenu.features.storage.dto.response.StorageApiKeyResponse;
import com.emenu.features.storage.models.StorageApiKey;
import com.emenu.shared.dto.PaginationResponse;

import java.util.UUID;

public interface StorageApiKeyService {

    /**
     * Create a new storage API key
     */
    StorageApiKeyCreatedResponse createApiKey(CreateStorageApiKeyRequest request);

    /**
     * Get API key by ID
     */
    StorageApiKeyResponse getApiKeyById(UUID id);

    /**
     * Get all API keys with pagination
     */
    PaginationResponse<StorageApiKeyResponse> getAllApiKeys(ApiKeyFilterRequest request);

    /**
     * Update API key
     */
    StorageApiKeyResponse updateApiKey(UUID id, UpdateStorageApiKeyRequest request);

    /**
     * Delete API key (soft delete)
     */
    void deleteApiKey(UUID id);

    /**
     * Regenerate API key
     */
    StorageApiKeyCreatedResponse regenerateApiKey(UUID id);

    /**
     * Validate API key and return the entity
     */
    StorageApiKey validateAndGetApiKey(String apiKey);

    /**
     * Toggle API key active status
     */
    StorageApiKeyResponse toggleApiKeyStatus(UUID id);
}
