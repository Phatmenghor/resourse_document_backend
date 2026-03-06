package com.emenu.features.appkey.controller;

import com.emenu.features.appkey.dto.request.AppKeyCreateRequest;
import com.emenu.features.appkey.dto.request.AppKeyUpdateRequest;
import com.emenu.features.appkey.dto.response.AppKeyResponse;
import com.emenu.features.appkey.service.AppKeyService;
import com.emenu.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/app-keys")
@RequiredArgsConstructor
public class AppKeyController {

    private final AppKeyService appKeyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<AppKeyResponse>> createAppKey(
            @Valid @RequestBody AppKeyCreateRequest request) {
        AppKeyResponse response = appKeyService.createAppKey(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("App key created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<AppKeyResponse>>> getAllAppKeys() {
        List<AppKeyResponse> response = appKeyService.getAllAppKeys();
        return ResponseEntity.ok(ApiResponse.success("App keys retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<AppKeyResponse>> getAppKeyById(@PathVariable UUID id) {
        AppKeyResponse response = appKeyService.getAppKeyById(id);
        return ResponseEntity.ok(ApiResponse.success("App key retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<AppKeyResponse>> updateAppKey(
            @PathVariable UUID id,
            @RequestBody AppKeyUpdateRequest request) {
        AppKeyResponse response = appKeyService.updateAppKey(id, request);
        return ResponseEntity.ok(ApiResponse.success("App key updated successfully", response));
    }

    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<AppKeyResponse>> regenerateApiKey(@PathVariable UUID id) {
        AppKeyResponse response = appKeyService.regenerateApiKey(id);
        return ResponseEntity.ok(ApiResponse.success("API key regenerated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAppKey(@PathVariable UUID id) {
        appKeyService.deleteAppKey(id);
        return ResponseEntity.ok(ApiResponse.success("App key deleted successfully", null));
    }
}
