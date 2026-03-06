package com.emenu.features.resource.controller;

import com.emenu.features.resource.dto.request.ResourceUploadRequest;
import com.emenu.features.resource.dto.response.ResourceCountResponse;
import com.emenu.features.resource.dto.response.ResourceFileResponse;
import com.emenu.features.resource.service.ResourceFileService;
import com.emenu.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceFileController {

    private final ResourceFileService resourceFileService;

    /**
     * Upload a file. Validates the API key, persists metadata, and queues the
     * actual disk-write via Kafka. Returns immediately with PENDING status.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResourceFileResponse>> upload(
            @Valid @RequestBody ResourceUploadRequest request) {
        ResourceFileResponse response = resourceFileService.upload(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File upload queued successfully", response));
    }

    /**
     * Multipart upload — accepts real file directly (no base64).
     * Status is COMPLETED immediately. Best for Swagger / testing.
     * File name format: ddMMyyyy_xxxxxxxx.ext (e.g. 06032026_a1b2c3d4.jpg)
     */
    @PostMapping(value = "/upload-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResourceFileResponse>> uploadMultipart(
            @RequestParam("key") String key,
            @RequestParam("resourceId") String resourceId,
            @RequestParam("file") MultipartFile file) {
        ResourceFileResponse response = resourceFileService.uploadMultipart(key, resourceId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", response));
    }

    /**
     * Preview / download a file by its ID.
     * Returns the raw bytes with the correct Content-Type.
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable UUID id) {
        ResourceFileResponse meta = resourceFileService.getById(id);
        byte[] data = resourceFileService.preview(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getMimeType()))
                .body(data);
    }

    /**
     * Get metadata for a single resource file.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceFileResponse>> getById(@PathVariable UUID id) {
        ResourceFileResponse response = resourceFileService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("Resource file retrieved", response));
    }

    /**
     * List all files belonging to a resourceId (business ID).
     */
    @GetMapping("/by-resource/{resourceId}")
    public ResponseEntity<ApiResponse<List<ResourceFileResponse>>> listByResourceId(
            @PathVariable String resourceId) {
        List<ResourceFileResponse> response = resourceFileService.listByResourceId(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource files retrieved", response));
    }

    /**
     * Count files for a given resourceId.
     */
    @GetMapping("/by-resource/{resourceId}/count")
    public ResponseEntity<ApiResponse<ResourceCountResponse>> countByResourceId(
            @PathVariable String resourceId) {
        ResourceCountResponse response = resourceFileService.countByResourceId(resourceId);
        return ResponseEntity.ok(ApiResponse.success("File count retrieved", response));
    }

    /**
     * Count files for a given application name.
     */
    @GetMapping("/by-app/{applicationName}/count")
    public ResponseEntity<ApiResponse<ResourceCountResponse>> countByApp(
            @PathVariable String applicationName) {
        ResourceCountResponse response = resourceFileService.countByApplicationName(applicationName);
        return ResponseEntity.ok(ApiResponse.success("File count retrieved", response));
    }

    /**
     * Delete a single file by ID.
     * Soft-deletes the DB record and schedules physical removal via Kafka.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable UUID id) {
        resourceFileService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    /**
     * Bulk-delete ALL files for a resourceId.
     * Ideal for when a business stops service – cleans up everything in one call.
     */
    @DeleteMapping("/by-resource/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> deleteAllByResourceId(
            @PathVariable String resourceId) {
        resourceFileService.deleteAllByResourceId(resourceId);
        return ResponseEntity.ok(
                ApiResponse.success("All files for resourceId '" + resourceId + "' deleted", null));
    }
}
