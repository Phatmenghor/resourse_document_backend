package com.emenu.features.resource.controller;

import com.emenu.features.appkey.models.AppKey;
import com.emenu.features.appkey.service.AppKeyService;
import com.emenu.features.resource.dto.request.DeleteBulkRequest;
import com.emenu.features.resource.dto.request.ResourceUploadBatchRequest;
import com.emenu.features.resource.dto.request.ResourceUploadRequest;
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

    // ─────────────────────── UPLOAD ───────────────────────────────

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResourceFileResponse>> upload(
            @Valid @RequestBody ResourceUploadRequest request) {
        ResourceFileResponse response = resourceFileService.upload(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File upload queued successfully", response));
    }

    @PostMapping(value = "/upload-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResourceFileResponse>> uploadMultipart(
            @RequestPart("key") String key,
            @RequestPart(value = "resourceId", required = false) String resourceId,
            @RequestPart("file") MultipartFile file) {
        ResourceFileResponse response = resourceFileService.uploadMultipart(key, resourceId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File upload queued successfully", response));
    }

    @PostMapping("/upload-batch")
    public ResponseEntity<ApiResponse<List<ResourceFileResponse>>> uploadBatch(
            @Valid @RequestBody ResourceUploadBatchRequest request) {
        List<ResourceFileResponse> responses = resourceFileService.uploadBatch(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Batch upload queued successfully", responses));
    }

    @PostMapping(value = "/upload-multipart-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ResourceFileResponse>>> uploadMultipartBatch(
            @RequestPart("key") String key,
            @RequestPart(value = "resourceId", required = false) String resourceId,
            @RequestPart("files") List<MultipartFile> files) {
        List<ResourceFileResponse> responses = resourceFileService.uploadMultipartBatch(key, resourceId, files);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Batch upload queued successfully", responses));
    }

    // ─────────────────────── PREVIEW ──────────────────────────────

    /**
     * Stream a file using its source path from the upload response.
     * GET /api/v1/resources/preview/my-app/2026-03-07/07032026_abc123.jpg
     */
    @GetMapping("/preview/{appName}/{date}/{filename}")
    public ResponseEntity<byte[]> preview(
            @PathVariable String appName,
            @PathVariable String date,
            @PathVariable String filename) {
        String filePath = appName + "/" + date + "/" + filename;
        byte[] data     = resourceFileService.preview(filePath);
        String mimeType = resolveMimeFromPath(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .body(data);
    }

    private String resolveMimeFromPath(String path) {
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".gif"))  return "image/gif";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".pdf"))  return "application/pdf";
        if (path.endsWith(".mp4"))  return "video/mp4";
        return "application/octet-stream";
    }

    // ─────────────────────── DELETE ───────────────────────────────

    /**
     * Delete a single file by filename (e.g. 07032026_d093324b.jpg).
     * DELETE /api/v1/resources/{filename}
     */
    @DeleteMapping("/{filename}")
    public ResponseEntity<ApiResponse<Void>> deleteByFilename(@PathVariable String filename) {
        resourceFileService.deleteByFilename(filename);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    /**
     * Bulk delete — provide exactly one of: resourceId, applicationName, apiKey.
     * POST /api/v1/resources/delete-bulk
     */
    @PostMapping("/delete-bulk")
    public ResponseEntity<ApiResponse<Void>> deleteBulk(@RequestBody DeleteBulkRequest request) {
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            AppKey appKey = appKeyService.validateAndGetAppKey(request.getApiKey());
            resourceFileService.deleteAllByApplicationName(appKey.getApplicationName());
            return ResponseEntity.ok(ApiResponse.success(
                    "All files for application '" + appKey.getApplicationName() + "' deleted", null));
        }
        if (request.getApplicationName() != null && !request.getApplicationName().isBlank()) {
            resourceFileService.deleteAllByApplicationName(request.getApplicationName());
            return ResponseEntity.ok(ApiResponse.success(
                    "All files for application '" + request.getApplicationName() + "' deleted", null));
        }
        if (request.getResourceId() != null && !request.getResourceId().isBlank()) {
            resourceFileService.deleteAllByResourceId(request.getResourceId());
            return ResponseEntity.ok(ApiResponse.success(
                    "All files for resourceId '" + request.getResourceId() + "' deleted", null));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Provide one of: resourceId, applicationName, or apiKey"));
    }
}
