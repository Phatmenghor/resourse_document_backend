package com.emenu.features.appkey.controller;

import com.emenu.features.appkey.dto.request.AppKeyCreateRequest;
import com.emenu.features.appkey.dto.request.AppKeyFilterRequest;
import com.emenu.features.appkey.dto.request.AppKeyUpdateRequest;
import com.emenu.features.appkey.dto.response.AppKeyResponse;
import com.emenu.features.appkey.service.AppKeyService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/app-keys")
@RequiredArgsConstructor
public class AppKeyController {

    private final AppKeyService appKeyService;

    /**
     * Create a new app key.
     * Body: { "applicationName": "my-app", "description": "optional" }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<AppKeyResponse>> createAppKey(
            @Valid @RequestBody AppKeyCreateRequest request) {
        AppKeyResponse response = appKeyService.createAppKey(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("App key created successfully", response));
    }

    /**
     * Search / list app keys with pagination.
     * Body: { "search": "my-app", "isActive": true, "pageNo": 1, "pageSize": 15, "sortBy": "createdAt", "sortDirection": "DESC" }
     * All fields are optional.
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<PaginationResponse<AppKeyResponse>>> searchAppKeys(
            @Valid @RequestBody AppKeyFilterRequest request) {
        PaginationResponse<AppKeyResponse> response = appKeyService.searchAppKeys(request);
        return ResponseEntity.ok(ApiResponse.success("App keys retrieved successfully", response));
    }

    /**
     * Get a single app key by its UUID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<AppKeyResponse>> getAppKeyById(@PathVariable UUID id) {
        AppKeyResponse response = appKeyService.getAppKeyById(id);
        return ResponseEntity.ok(ApiResponse.success("App key retrieved successfully", response));
    }

    /**
     * Update description or active status.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<AppKeyResponse>> updateAppKey(
            @PathVariable UUID id,
            @RequestBody AppKeyUpdateRequest request) {
        AppKeyResponse response = appKeyService.updateAppKey(id, request);
        return ResponseEntity.ok(ApiResponse.success("App key updated successfully", response));
    }

    /**
     * Regenerate the secret API key value (keeps same applicationName).
     */
    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<AppKeyResponse>> regenerateApiKey(@PathVariable UUID id) {
        AppKeyResponse response = appKeyService.regenerateApiKey(id);
        return ResponseEntity.ok(ApiResponse.success("API key regenerated successfully", response));
    }

    /**
     * Soft-delete an app key by its UUID.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAppKey(@PathVariable UUID id) {
        appKeyService.deleteAppKey(id);
        return ResponseEntity.ok(ApiResponse.success("App key deleted successfully", null));
    }
}
