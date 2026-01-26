package com.emenu.features.storage.dto.response;

import com.emenu.enums.storage.ResourceType;
import com.emenu.enums.storage.StorageStatus;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResourceFileResponse extends BaseAuditResponse {

    private UUID apiKeyId;

    private String apiKeyName;

    private String originalFilename;

    private String storedFilename;

    private String storagePath;

    private ResourceType resourceType;

    private String mimeType;

    private Long fileSizeBytes;

    private String fileSizeFormatted;

    private Boolean isEncrypted;

    private StorageStatus status;

    private Boolean isTemporary;

    private LocalDateTime expiresAt;

    private String checksum;

    private Long downloadCount;

    private LocalDateTime lastAccessedAt;

    private String metadata;

    private String downloadUrl;
}
