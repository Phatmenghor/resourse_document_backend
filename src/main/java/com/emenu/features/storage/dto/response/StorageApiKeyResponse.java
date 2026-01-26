package com.emenu.features.storage.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorageApiKeyResponse extends BaseAuditResponse {

    private String name;

    private String apiKeyPrefix; // Only show prefix, not full key

    private String description;

    private String storagePath;

    private Boolean isActive;

    private Long maxFileSizeMb;

    private Long maxStorageMb;

    private Long currentStorageBytes;

    private Long totalUploadCount;

    private Long totalDownloadCount;

    private String allowedFileTypes;

    // Calculated fields
    private Double storageUsedPercentage;
    private String currentStorageFormatted;
    private String maxStorageFormatted;
}
