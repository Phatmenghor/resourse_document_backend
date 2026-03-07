package com.emenu.features.resource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResourceUploadRequest {

    @NotBlank(message = "API key is required")
    private String key;

    /** Optional — may be null if the file is not tied to a specific resource. */
    private String resourceId;

    /**
     * Base64 encoded file content. May optionally include the data URI prefix
     * (e.g. "data:image/jpeg;base64,..."), which will be stripped automatically.
     * MIME type is detected from the prefix when present.
     */
    @NotBlank(message = "Base64 file data is required")
    private String base64;
}
