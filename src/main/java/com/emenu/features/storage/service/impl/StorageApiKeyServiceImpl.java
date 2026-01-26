package com.emenu.features.storage.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.storage.dto.filter.ApiKeyFilterRequest;
import com.emenu.features.storage.dto.request.CreateStorageApiKeyRequest;
import com.emenu.features.storage.dto.request.UpdateStorageApiKeyRequest;
import com.emenu.features.storage.dto.response.StorageApiKeyCreatedResponse;
import com.emenu.features.storage.dto.response.StorageApiKeyResponse;
import com.emenu.features.storage.mapper.StorageApiKeyMapper;
import com.emenu.features.storage.models.StorageApiKey;
import com.emenu.features.storage.repository.StorageApiKeyRepository;
import com.emenu.features.storage.service.StorageApiKeyService;
import com.emenu.features.storage.util.StorageUtils;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.pagination.PaginationUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StorageApiKeyServiceImpl implements StorageApiKeyService {

    private final StorageApiKeyRepository apiKeyRepository;
    private final StorageApiKeyMapper apiKeyMapper;
    private final PasswordEncoder passwordEncoder;

    private static final int API_KEY_LENGTH = 32;
    private static final int PREFIX_LENGTH = 8;

    @Override
    public StorageApiKeyCreatedResponse createApiKey(CreateStorageApiKeyRequest request) {
        log.info("Creating storage API key with name: {}", request.getName());

        // Validate unique name
        if (apiKeyRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new ValidationException("API key with name '" + request.getName() + "' already exists");
        }

        // Generate API key
        String rawApiKey = generateRawApiKey();
        String hashedApiKey = passwordEncoder.encode(rawApiKey);
        String apiKeyPrefix = rawApiKey.substring(0, PREFIX_LENGTH);

        // Create entity
        StorageApiKey apiKey = apiKeyMapper.toEntity(request);
        apiKey.setApiKey(hashedApiKey);
        apiKey.setApiKeyPrefix(apiKeyPrefix);
        apiKey.setStoragePath(StorageUtils.sanitizePathName(request.getName()));

        // Save
        StorageApiKey saved = apiKeyRepository.save(apiKey);

        // Create response with raw API key (only shown once)
        StorageApiKeyCreatedResponse response = apiKeyMapper.toCreatedResponse(saved);
        response.setApiKey(rawApiKey); // Return the raw key only at creation

        log.info("Storage API key created with ID: {}", saved.getId());
        return response;
    }

    @Override
    public StorageApiKeyResponse getApiKeyById(UUID id) {
        log.info("Getting storage API key by ID: {}", id);
        StorageApiKey apiKey = findApiKeyById(id);
        return apiKeyMapper.toResponse(apiKey);
    }

    @Override
    public PaginationResponse<StorageApiKeyResponse> getAllApiKeys(ApiKeyFilterRequest request) {
        log.info("Getting all storage API keys with filter");

        Pageable pageable = PaginationUtils.createPageable(
                request.getPageNo(),
                request.getPageSize(),
                request.getSortBy(),
                request.getSortDirection()
        );

        Page<StorageApiKey> page = apiKeyRepository.searchApiKeys(
                request.getName(),
                request.getIsActive(),
                pageable
        );

        return apiKeyMapper.toPaginationResponse(page);
    }

    @Override
    public StorageApiKeyResponse updateApiKey(UUID id, UpdateStorageApiKeyRequest request) {
        log.info("Updating storage API key: {}", id);

        StorageApiKey apiKey = findApiKeyById(id);
        apiKeyMapper.updateEntity(request, apiKey);
        StorageApiKey saved = apiKeyRepository.save(apiKey);

        log.info("Storage API key updated: {}", id);
        return apiKeyMapper.toResponse(saved);
    }

    @Override
    public void deleteApiKey(UUID id) {
        log.info("Deleting storage API key: {}", id);

        StorageApiKey apiKey = findApiKeyById(id);
        apiKey.softDelete("SYSTEM");
        apiKeyRepository.save(apiKey);

        log.info("Storage API key deleted: {}", id);
    }

    @Override
    public StorageApiKeyCreatedResponse regenerateApiKey(UUID id) {
        log.info("Regenerating storage API key: {}", id);

        StorageApiKey apiKey = findApiKeyById(id);

        // Generate new API key
        String rawApiKey = generateRawApiKey();
        String hashedApiKey = passwordEncoder.encode(rawApiKey);
        String apiKeyPrefix = rawApiKey.substring(0, PREFIX_LENGTH);

        apiKey.setApiKey(hashedApiKey);
        apiKey.setApiKeyPrefix(apiKeyPrefix);

        StorageApiKey saved = apiKeyRepository.save(apiKey);

        // Create response with raw API key
        StorageApiKeyCreatedResponse response = apiKeyMapper.toCreatedResponse(saved);
        response.setApiKey(rawApiKey);

        log.info("Storage API key regenerated: {}", id);
        return response;
    }

    @Override
    public StorageApiKey validateAndGetApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ValidationException("API key is required");
        }

        // Extract prefix for faster lookup
        if (apiKey.length() < PREFIX_LENGTH) {
            throw new ValidationException("Invalid API key format");
        }

        String prefix = apiKey.substring(0, PREFIX_LENGTH);
        StorageApiKey storedKey = apiKeyRepository.findByApiKeyPrefix(prefix)
                .orElseThrow(() -> new ValidationException("Invalid API key"));

        // Verify full key
        if (!passwordEncoder.matches(apiKey, storedKey.getApiKey())) {
            throw new ValidationException("Invalid API key");
        }

        // Check if active
        if (!Boolean.TRUE.equals(storedKey.getIsActive())) {
            throw new ValidationException("API key is not active");
        }

        // Check if deleted
        if (Boolean.TRUE.equals(storedKey.getIsDeleted())) {
            throw new ValidationException("API key has been deleted");
        }

        return storedKey;
    }

    @Override
    public StorageApiKeyResponse toggleApiKeyStatus(UUID id) {
        log.info("Toggling storage API key status: {}", id);

        StorageApiKey apiKey = findApiKeyById(id);
        apiKey.setIsActive(!Boolean.TRUE.equals(apiKey.getIsActive()));
        StorageApiKey saved = apiKeyRepository.save(apiKey);

        log.info("Storage API key status toggled to: {} for ID: {}", saved.getIsActive(), id);
        return apiKeyMapper.toResponse(saved);
    }

    private StorageApiKey findApiKeyById(UUID id) {
        return apiKeyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Storage API key not found with ID: " + id));
    }

    private String generateRawApiKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[API_KEY_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
