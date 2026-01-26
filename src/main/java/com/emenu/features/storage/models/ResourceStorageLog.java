package com.emenu.features.storage.models;

import com.emenu.enums.storage.StorageLogAction;
import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "resource_storage_logs", indexes = {
        @Index(name = "idx_storage_log_api_key", columnList = "api_key_id, is_deleted"),
        @Index(name = "idx_storage_log_resource", columnList = "resource_file_id, is_deleted"),
        @Index(name = "idx_storage_log_action", columnList = "action, is_deleted"),
        @Index(name = "idx_storage_log_timestamp", columnList = "action_timestamp, is_deleted"),
        @Index(name = "idx_storage_log_api_action", columnList = "api_key_id, action, is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceStorageLog extends BaseUUIDEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private StorageApiKey apiKey;

    @Column(name = "resource_file_id")
    private UUID resourceFileId; // Can be null for failed operations

    @Column(name = "original_filename")
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private StorageLogAction action;

    @Column(name = "action_timestamp", nullable = false)
    private LocalDateTime actionTimestamp;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "request_id")
    private String requestId; // For tracking requests

    @Column(name = "duration_ms")
    private Long durationMs; // Request duration in milliseconds

    public static ResourceStorageLog createLog(StorageApiKey apiKey, StorageLogAction action) {
        ResourceStorageLog log = new ResourceStorageLog();
        log.setApiKey(apiKey);
        log.setAction(action);
        log.setActionTimestamp(LocalDateTime.now());
        log.setIsSuccess(true);
        return log;
    }

    public static ResourceStorageLog createErrorLog(StorageApiKey apiKey, StorageLogAction action, String errorMessage) {
        ResourceStorageLog log = createLog(apiKey, action);
        log.setIsSuccess(false);
        log.setErrorMessage(errorMessage);
        return log;
    }
}
