package com.emenu.features.storage.models;

import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "storage_api_keys", indexes = {
        @Index(name = "idx_storage_api_key_name", columnList = "name, is_deleted"),
        @Index(name = "idx_storage_api_key_key", columnList = "api_key, is_deleted"),
        @Index(name = "idx_storage_api_key_active", columnList = "is_active, is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageApiKey extends BaseUUIDEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name; // e.g., "account_online", "mobile_app"

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey; // The actual API key (hashed)

    @Column(name = "api_key_prefix", nullable = false)
    private String apiKeyPrefix; // First 8 chars for identification

    @Column(name = "description")
    private String description;

    @Column(name = "storage_path", nullable = false)
    private String storagePath; // Path prefix for this API key's resources

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "max_file_size_mb")
    private Long maxFileSizeMb = 10L; // Default 10MB max file size

    @Column(name = "max_storage_mb")
    private Long maxStorageMb = 1024L; // Default 1GB max total storage

    @Column(name = "current_storage_bytes")
    private Long currentStorageBytes = 0L;

    @Column(name = "total_upload_count")
    private Long totalUploadCount = 0L;

    @Column(name = "total_download_count")
    private Long totalDownloadCount = 0L;

    @Column(name = "allowed_file_types")
    private String allowedFileTypes; // Comma-separated: "IMAGE,PDF,DOCUMENT"

    public void incrementUploadCount() {
        this.totalUploadCount = (this.totalUploadCount == null ? 0L : this.totalUploadCount) + 1;
    }

    public void incrementDownloadCount() {
        this.totalDownloadCount = (this.totalDownloadCount == null ? 0L : this.totalDownloadCount) + 1;
    }

    public void addStorageBytes(long bytes) {
        this.currentStorageBytes = (this.currentStorageBytes == null ? 0L : this.currentStorageBytes) + bytes;
    }

    public void subtractStorageBytes(long bytes) {
        this.currentStorageBytes = Math.max(0L, (this.currentStorageBytes == null ? 0L : this.currentStorageBytes) - bytes);
    }
}
