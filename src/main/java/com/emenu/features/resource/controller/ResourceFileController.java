package com.emenu.features.resource.controller;

import com.emenu.features.appkey.models.AppKey;
import com.emenu.features.appkey.service.AppKeyService;
import com.emenu.features.resource.dto.request.ApiKeyRequest;
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
    private final AppKeyService appKeyService;

    /**
     * Upload a file via base64.
     * Body: { "key": "...", "resourceId": "...(optional)", "mimeType": "image/jpeg", "base64": "..." }
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
     * Form fields: key (text), resourceId (text, optional), file (binary)
     */
    @PostMapping(value = "/upload-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResourceFileResponse>> uploadMultipart(
            @RequestPart("key") String key,
            @RequestPart(value = "resourceId", required = false) String resourceId,
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
     * Body: { "id": "uuid" }
     */
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteById(
            @Valid @RequestBody ResourceFileIdRequest request) {
        resourceFileService.deleteById(request.getId());
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    /**
     * Bulk-delete all files for a resourceId.
     * Body: { "resourceId": "..." }
     */
    @PostMapping("/delete-by-resource")
    public ResponseEntity<ApiResponse<Void>> deleteAllByResourceId(
            @Valid @RequestBody ResourceIdRequest request) {
        resourceFileService.deleteAllByResourceId(request.getResourceId());
        return ResponseEntity.ok(
                ApiResponse.success("All files for resourceId '" + request.getResourceId() + "' deleted", null));
    }

    /**
     * Bulk-delete all files belonging to an application name.
     * Body: { "applicationName": "..." }
     */
    @PostMapping("/delete-by-app")
    public ResponseEntity<ApiResponse<Void>> deleteAllByApp(
            @Valid @RequestBody AppNameRequest request) {
        resourceFileService.deleteAllByApplicationName(request.getApplicationName());
        return ResponseEntity.ok(
                ApiResponse.success("All files for application '" + request.getApplicationName() + "' deleted", null));
    }

    /**
     * Bulk-delete all files whose API key has been stopped/revoked.
     * Looks up the applicationName from the key, then deletes all its files.
     * Body: { "apiKey": "rk_abc123..." }
     */
    @PostMapping("/delete-by-api-key")
    public ResponseEntity<ApiResponse<Void>> deleteAllByApiKey(
            @Valid @RequestBody ApiKeyRequest request) {
        AppKey appKey = appKeyService.validateAndGetAppKey(request.getApiKey());
        resourceFileService.deleteAllByApplicationName(appKey.getApplicationName());
        return ResponseEntity.ok(
                ApiResponse.success(
                        "All files for application '" + appKey.getApplicationName() + "' deleted", null));
    }
}
