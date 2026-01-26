package com.emenu.features.storage.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorageApiKeyCreatedResponse extends BaseAuditResponse {

    private String name;

    private String apiKey; // Full API key - only shown once at creation

    private String apiKeyPrefix;

    private String description;

    private String storagePath;

    private Boolean isActive;

    private Long maxFileSizeMb;

    private Long maxStorageMb;

    private String allowedFileTypes;
}
