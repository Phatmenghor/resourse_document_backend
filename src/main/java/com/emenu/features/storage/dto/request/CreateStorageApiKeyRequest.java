package com.emenu.features.storage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStorageApiKeyRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name; // e.g., "account_online", "mobile_app"

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private Long maxFileSizeMb = 10L; // Default 10MB

    private Long maxStorageMb = 1024L; // Default 1GB

    private String allowedFileTypes; // Comma-separated: "IMAGE,PDF,DOCUMENT"
}
