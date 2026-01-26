package com.emenu.features.storage.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStorageApiKeyRequest {

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private Long maxFileSizeMb;

    private Long maxStorageMb;

    private String allowedFileTypes;

    private Boolean isActive;
}
