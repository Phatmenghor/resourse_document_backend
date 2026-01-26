package com.emenu.features.storage.controller;

import com.emenu.features.storage.dto.filter.ApiKeyFilterRequest;
import com.emenu.features.storage.dto.request.CreateStorageApiKeyRequest;
import com.emenu.features.storage.dto.request.UpdateStorageApiKeyRequest;
import com.emenu.features.storage.dto.response.StorageApiKeyCreatedResponse;
import com.emenu.features.storage.dto.response.StorageApiKeyResponse;
import com.emenu.features.storage.service.StorageApiKeyService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage/api-keys")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Storage API Key Management", description = "APIs for managing storage API keys")
public class StorageApiKeyController {

    private final StorageApiKeyService apiKeyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new storage API key")
    public ResponseEntity<ApiResponse<StorageApiKeyCreatedResponse>> createApiKey(
            @Valid @RequestBody CreateStorageApiKeyRequest request) {
        log.info("Creating storage API key: {}", request.getName());
        StorageApiKeyCreatedResponse response = apiKeyService.createApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Storage API key created successfully. Save the API key, it won't be shown again.", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get storage API key by ID")
    public ResponseEntity<ApiResponse<StorageApiKeyResponse>> getApiKeyById(@PathVariable UUID id) {
        log.info("Getting storage API key: {}", id);
        StorageApiKeyResponse response = apiKeyService.getApiKeyById(id);
        return ResponseEntity.ok(ApiResponse.success("Storage API key retrieved successfully", response));
    }

    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all storage API keys with pagination")
    public ResponseEntity<ApiResponse<PaginationResponse<StorageApiKeyResponse>>> getAllApiKeys(
            @Valid @RequestBody ApiKeyFilterRequest request) {
        log.info("Getting all storage API keys");
        PaginationResponse<StorageApiKeyResponse> response = apiKeyService.getAllApiKeys(request);
        return ResponseEntity.ok(ApiResponse.success("Storage API keys retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update storage API key")
    public ResponseEntity<ApiResponse<StorageApiKeyResponse>> updateApiKey(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStorageApiKeyRequest request) {
        log.info("Updating storage API key: {}", id);
        StorageApiKeyResponse response = apiKeyService.updateApiKey(id, request);
        return ResponseEntity.ok(ApiResponse.success("Storage API key updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete storage API key")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(@PathVariable UUID id) {
        log.info("Deleting storage API key: {}", id);
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.ok(ApiResponse.success("Storage API key deleted successfully", null));
    }

    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Regenerate storage API key")
    public ResponseEntity<ApiResponse<StorageApiKeyCreatedResponse>> regenerateApiKey(@PathVariable UUID id) {
        log.info("Regenerating storage API key: {}", id);
        StorageApiKeyCreatedResponse response = apiKeyService.regenerateApiKey(id);
        return ResponseEntity.ok(ApiResponse.success("Storage API key regenerated successfully. Save the new API key, it won't be shown again.", response));
    }

    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle storage API key active status")
    public ResponseEntity<ApiResponse<StorageApiKeyResponse>> toggleApiKeyStatus(@PathVariable UUID id) {
        log.info("Toggling storage API key status: {}", id);
        StorageApiKeyResponse response = apiKeyService.toggleApiKeyStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Storage API key status toggled successfully", response));
    }
}
