package com.emenu.features.storage.dto.response;

import com.emenu.enums.storage.StorageLogAction;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResourceStorageLogResponse extends BaseAuditResponse {

    private UUID apiKeyId;

    private String apiKeyName;

    private UUID resourceFileId;

    private String originalFilename;

    private StorageLogAction action;

    private LocalDateTime actionTimestamp;

    private Long fileSizeBytes;

    private String fileSizeFormatted;

    private Boolean isSuccess;

    private String errorMessage;

    private String clientIp;

    private String userAgent;

    private String requestId;

    private Long durationMs;
}
