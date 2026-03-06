package com.emenu.features.resource.controller;

import com.emenu.features.resource.dto.request.AppNameRequest;
import com.emenu.features.resource.dto.request.ResourceFileIdRequest;
import com.emenu.features.resource.dto.request.ResourceIdRequest;
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

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceFileController {

    private final ResourceFileService resourceFileService;

    /**
     * Upload a file via base64.
     * Persists metadata with PENDING status and queues the disk-write via Kafka.
     *
     * Body: { "key": "...", "resourceId": "...", "mimeType": "image/jpeg", "base64": "..." }
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResourceFileResponse>> upload(
            @Valid @RequestBody ResourceUploadRequest request) {
        ResourceFileResponse response = resourceFileService.upload(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File upload queued successfully", response));
    }

    /**
     * Upload a file via multipart form.
     * Converts bytes to base64 and queues the disk-write via Kafka (PENDING status, async).
     *
     * Form fields: key (text), resourceId (text), file (binary)
     */
    @PostMapping(value = "/upload-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResourceFileResponse>> uploadMultipart(
            @RequestPart("key") String key,
            @RequestPart("resourceId") String resourceId,
            @RequestPart("file") MultipartFile file) {
        ResourceFileResponse response = resourceFileService.uploadMultipart(key, resourceId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File upload queued successfully", response));
    }

    /**
     * Preview / download a file.
     * Body: { "id": "uuid" }
     */
    @PostMapping("/preview")
    public ResponseEntity<byte[]> preview(@Valid @RequestBody ResourceFileIdRequest request) {
        ResourceFileResponse meta = resourceFileService.getById(request.getId());
        byte[] data = resourceFileService.preview(request.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getMimeType()))
                .body(data);
    }

    /**
     * Get metadata for a single resource file.
     * Body: { "id": "uuid" }
     */
    @PostMapping("/get")
    public ResponseEntity<ApiResponse<ResourceFileResponse>> getById(
            @Valid @RequestBody ResourceFileIdRequest request) {
        ResourceFileResponse response = resourceFileService.getById(request.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource file retrieved", response));
    }

    /**
     * List all files belonging to a resourceId.
     * Body: { "resourceId": "..." }
     */
    @PostMapping("/by-resource")
    public ResponseEntity<ApiResponse<List<ResourceFileResponse>>> listByResourceId(
            @Valid @RequestBody ResourceIdRequest request) {
        List<ResourceFileResponse> response = resourceFileService.listByResourceId(request.getResourceId());
        return ResponseEntity.ok(ApiResponse.success("Resource files retrieved", response));
    }

    /**
     * Count files for a given resourceId.
     * Body: { "resourceId": "..." }
     */
    @PostMapping("/by-resource/count")
    public ResponseEntity<ApiResponse<ResourceCountResponse>> countByResourceId(
            @Valid @RequestBody ResourceIdRequest request) {
        ResourceCountResponse response = resourceFileService.countByResourceId(request.getResourceId());
        return ResponseEntity.ok(ApiResponse.success("File count retrieved", response));
    }

    /**
     * Count files for a given application name.
     * Body: { "applicationName": "..." }
     */
    @PostMapping("/by-app/count")
    public ResponseEntity<ApiResponse<ResourceCountResponse>> countByApp(
            @Valid @RequestBody AppNameRequest request) {
        ResourceCountResponse response = resourceFileService.countByApplicationName(request.getApplicationName());
        return ResponseEntity.ok(ApiResponse.success("File count retrieved", response));
    }

    /**
     * Delete a single file by ID.
     * Soft-deletes DB record and schedules physical removal via Kafka.
     * Body: { "id": "uuid" }
     */
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteById(
            @Valid @RequestBody ResourceFileIdRequest request) {
        resourceFileService.deleteById(request.getId());
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    /**
     * Bulk-delete ALL files for a resourceId via Kafka.
     * Body: { "resourceId": "..." }
     */
    @PostMapping("/delete-by-resource")
    public ResponseEntity<ApiResponse<Void>> deleteAllByResourceId(
            @Valid @RequestBody ResourceIdRequest request) {
        resourceFileService.deleteAllByResourceId(request.getResourceId());
        return ResponseEntity.ok(
                ApiResponse.success("All files for resourceId '" + request.getResourceId() + "' deleted", null));
    }
}
