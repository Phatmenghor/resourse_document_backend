package com.emenu.features.storage.controller;

import com.emenu.features.storage.dto.filter.ResourceFilterRequest;
import com.emenu.features.storage.dto.filter.StorageLogFilterRequest;
import com.emenu.features.storage.dto.request.UploadResourceRequest;
import com.emenu.features.storage.dto.response.*;
import com.emenu.features.storage.service.ResourceStorageService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage/resources")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resource Storage", description = "APIs for uploading and downloading resources")
public class ResourceStorageController {

    private final ResourceStorageService resourceStorageService;

    private static final String API_KEY_HEADER = "X-Storage-Api-Key";

    @PostMapping("/upload")
    @Operation(summary = "Upload a resource file")
    public ResponseEntity<ApiResponse<ResourceUploadResponse>> uploadResource(
            @Parameter(description = "Storage API Key", required = true)
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @Valid @RequestBody UploadResourceRequest request,
            HttpServletRequest httpRequest) {
        log.info("Uploading resource: {}", request.getFilename());
        ResourceUploadResponse response = resourceStorageService.uploadResource(apiKey, request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource uploaded successfully", response));
    }

    @GetMapping("/{resourceId}/download")
    @Operation(summary = "Download a resource file")
    public ResponseEntity<byte[]> downloadResource(
            @Parameter(description = "Storage API Key", required = true)
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @PathVariable UUID resourceId,
            HttpServletRequest httpRequest) {
        log.info("Downloading resource: {}", resourceId);
        ResourceDownloadResponse response = resourceStorageService.downloadResource(apiKey, resourceId, httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + response.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(response.getMimeType()))
                .contentLength(response.getFileSizeBytes())
                .body(response.getData());
    }

    @GetMapping("/{resourceId}/view")
    @Operation(summary = "View/stream a resource file (inline)")
    public ResponseEntity<byte[]> viewResource(
            @Parameter(description = "Storage API Key", required = true)
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @PathVariable UUID resourceId,
            HttpServletRequest httpRequest) {
        log.info("Viewing resource: {}", resourceId);
        ResourceDownloadResponse response = resourceStorageService.downloadResource(apiKey, resourceId, httpRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + response.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(response.getMimeType()))
                .contentLength(response.getFileSizeBytes())
                .body(response.getData());
    }

    @GetMapping("/{resourceId}")
    @Operation(summary = "Get resource metadata")
    public ResponseEntity<ApiResponse<ResourceFileResponse>> getResourceMetadata(
            @Parameter(description = "Storage API Key", required = true)
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @PathVariable UUID resourceId) {
        log.info("Getting resource metadata: {}", resourceId);
        ResourceFileResponse response = resourceStorageService.getResourceMetadata(apiKey, resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource metadata retrieved successfully", response));
    }

    @PostMapping("/list")
    @Operation(summary = "List all resources for the API key")
    public ResponseEntity<ApiResponse<PaginationResponse<ResourceFileResponse>>> listResources(
            @Parameter(description = "Storage API Key", required = true)
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @Valid @RequestBody ResourceFilterRequest request) {
        log.info("Listing resources");
        PaginationResponse<ResourceFileResponse> response = resourceStorageService.listResources(apiKey, request);
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", response));
    }

    @DeleteMapping("/{resourceId}")
    @Operation(summary = "Delete a resource")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @Parameter(description = "Storage API Key", required = true)
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @PathVariable UUID resourceId,
            HttpServletRequest httpRequest) {
        log.info("Deleting resource: {}", resourceId);
        resourceStorageService.deleteResource(apiKey, resourceId, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully", null));
    }

    @PostMapping("/logs")
    @Operation(summary = "Get storage logs for the API key")
    public ResponseEntity<ApiResponse<PaginationResponse<ResourceStorageLogResponse>>> getStorageLogs(
            @Parameter(description = "Storage API Key", required = true)
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @Valid @RequestBody StorageLogFilterRequest request) {
        log.info("Getting storage logs");
        PaginationResponse<ResourceStorageLogResponse> response = resourceStorageService.getStorageLogs(apiKey, request);
        return ResponseEntity.ok(ApiResponse.success("Storage logs retrieved successfully", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get storage statistics for the API key")
    public ResponseEntity<ApiResponse<StorageStatsResponse>> getStorageStats(
            @Parameter(description = "Storage API Key", required = true)
            @RequestHeader(API_KEY_HEADER) String apiKey) {
        log.info("Getting storage stats");
        StorageStatsResponse response = resourceStorageService.getStorageStats(apiKey);
        return ResponseEntity.ok(ApiResponse.success("Storage statistics retrieved successfully", response));
    }

    // Public endpoint for viewing resources (no API key required, used for sharing)
    @GetMapping("/public/{resourceId}")
    @Operation(summary = "Public endpoint to view/download a resource")
    public ResponseEntity<byte[]> publicDownload(@PathVariable UUID resourceId) {
        log.info("Public download for resource: {}", resourceId);
        ResourceDownloadResponse response = resourceStorageService.publicDownload(resourceId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + response.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(response.getMimeType()))
                .contentLength(response.getFileSizeBytes())
                .body(response.getData());
    }
}
