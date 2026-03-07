package com.emenu.features.resource.controller;

import com.emenu.features.resource.dto.request.DeleteBulkRequest;
import com.emenu.features.resource.dto.request.ResourceUploadBatchRequest;
import com.emenu.features.resource.dto.request.ResourceUploadRequest;
import com.emenu.features.resource.dto.response.ResourceFileResponse;
import com.emenu.features.resource.service.ResourceFileService;
import com.emenu.features.resource.utils.FileUtils;
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

    // ─────────────────────── UPLOAD ───────────────────────────────

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResourceFileResponse>> upload(
            @Valid @RequestBody ResourceUploadRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File upload queued successfully", resourceFileService.upload(request)));
    }

    @PostMapping(value = "/upload-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResourceFileResponse>> uploadMultipart(
            @RequestPart("key") String key,
            @RequestPart(value = "resourceId", required = false) String resourceId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File upload queued successfully", resourceFileService.uploadMultipart(key, resourceId, file)));
    }

    @PostMapping("/upload-batch")
    public ResponseEntity<ApiResponse<List<ResourceFileResponse>>> uploadBatch(
            @Valid @RequestBody ResourceUploadBatchRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Batch upload queued successfully", resourceFileService.uploadBatch(request)));
    }

    @PostMapping(value = "/upload-multipart-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ResourceFileResponse>>> uploadMultipartBatch(
            @RequestPart("key") String key,
            @RequestPart(value = "resourceId", required = false) String resourceId,
            @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Batch upload queued successfully", resourceFileService.uploadMultipartBatch(key, resourceId, files)));
    }

    // ─────────────────────── PREVIEW ──────────────────────────────

    @GetMapping("/preview/{appName}/{date}/{filename}")
    public ResponseEntity<byte[]> preview(
            @PathVariable String appName,
            @PathVariable String date,
            @PathVariable String filename) {
        String filePath = FileUtils.buildFilePath(appName, date, filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FileUtils.mimeFromPath(filePath)))
                .body(resourceFileService.preview(filePath));
    }

    // ─────────────────────── DELETE ───────────────────────────────

    @DeleteMapping("/{appName}/{date}/{filename}")
    public ResponseEntity<ApiResponse<Void>> deleteByPath(
            @PathVariable String appName,
            @PathVariable String date,
            @PathVariable String filename) {
        resourceFileService.deleteByFilePath(FileUtils.buildFilePath(appName, date, filename));
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    @PostMapping("/delete-bulk")
    public ResponseEntity<ApiResponse<Void>> deleteBulk(@RequestBody DeleteBulkRequest request) {
        resourceFileService.deleteBulk(request);
        return ResponseEntity.ok(ApiResponse.success("Files deleted successfully", null));
    }
}
