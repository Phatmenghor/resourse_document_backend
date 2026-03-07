package com.emenu.features.resource.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ResourceUploadBatchRequest {

    @NotBlank(message = "API key is required")
    private String key;

    /** Optional — shared resourceId for all files in this batch. */
    private String resourceId;

    /**
     * List of base64-encoded files. Each entry may optionally include the data URI prefix
     * (e.g. "data:image/jpeg;base64,..."), which will be stripped automatically.
     */
    @NotEmpty(message = "At least one file is required")
    private List<String> files;
}
