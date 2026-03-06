package com.emenu.features.resource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResourceUploadRequest {

    /**
     * API key to identify the application uploading the file.
     */
    @NotBlank(message = "API key is required")
    private String key;

    /**
     * Business/resource identifier for grouping (e.g. business ID).
     * Used to bulk-delete all files belonging to a resource.
     */
    @NotBlank(message = "Resource ID is required")
    private String resourceId;

    /**
     * Original file name including extension (e.g. "invoice.pdf", "photo.jpg").
     */
    @NotBlank(message = "File name is required")
    private String fileName;

    /**
     * MIME type of the file (e.g. "image/jpeg", "application/pdf").
     */
    @NotBlank(message = "MIME type is required")
    private String mimeType;

    /**
     * Base64 encoded file content. May optionally include the data URI prefix
     * (e.g. "data:image/jpeg;base64,..."), which will be stripped automatically.
     */
    @NotBlank(message = "Base64 file data is required")
    private String base64;
}
