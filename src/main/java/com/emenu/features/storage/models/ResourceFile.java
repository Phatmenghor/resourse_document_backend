package com.emenu.features.storage.models;

import com.emenu.enums.storage.ResourceType;
import com.emenu.enums.storage.StorageStatus;
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
@Table(name = "resource_files", indexes = {
        @Index(name = "idx_resource_file_api_key", columnList = "api_key_id, is_deleted"),
        @Index(name = "idx_resource_file_status", columnList = "status, is_deleted"),
        @Index(name = "idx_resource_file_type", columnList = "resource_type, is_deleted"),
        @Index(name = "idx_resource_file_expires", columnList = "expires_at, status, is_deleted"),
        @Index(name = "idx_resource_file_path", columnList = "storage_path, is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFile extends BaseUUIDEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private StorageApiKey apiKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename; // UUID-based filename

    @Column(name = "storage_path", nullable = false)
    private String storagePath; // Full path including API key prefix

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Lob
    @Column(name = "encrypted_data", columnDefinition = "TEXT")
    private String encryptedData; // Base64 encoded encrypted content

    @Column(name = "encryption_iv")
    private String encryptionIv; // Initialization vector for decryption

    @Column(name = "is_encrypted", nullable = false)
    private Boolean isEncrypted = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StorageStatus status = StorageStatus.ACTIVE;

    @Column(name = "is_temporary", nullable = false)
    private Boolean isTemporary = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // For temporary files

    @Column(name = "checksum")
    private String checksum; // SHA-256 hash of original file

    @Column(name = "download_count")
    private Long downloadCount = 0L;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional metadata

    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0L : this.downloadCount) + 1;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        if (!Boolean.TRUE.equals(isTemporary) || expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
