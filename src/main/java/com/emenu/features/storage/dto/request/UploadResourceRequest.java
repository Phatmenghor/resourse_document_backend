package com.emenu.features.storage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UploadResourceRequest {

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "Content type is required")
    private String contentType; // MIME type: image/png, application/pdf, etc.

    @NotBlank(message = "Data is required")
    private String data; // Base64 encoded file content

    private Boolean isTemporary = false;

    private Long expiresInMinutes; // Expiration time for temporary files (e.g., 60 for 1 hour)

    private String metadata; // Optional JSON metadata
}
