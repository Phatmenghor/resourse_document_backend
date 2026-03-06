package com.emenu.features.resource.models;

import com.emenu.enums.resource.FileStatus;
import com.emenu.enums.resource.FileType;
import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "resource_files", indexes = {
        @Index(name = "idx_resource_file_deleted", columnList = "is_deleted"),
        @Index(name = "idx_resource_file_resource_id", columnList = "resource_id"),
        @Index(name = "idx_resource_file_app_name", columnList = "application_name"),
        @Index(name = "idx_resource_file_status", columnList = "status"),
        @Index(name = "idx_resource_file_resource_id_deleted", columnList = "resource_id, is_deleted"),
        @Index(name = "idx_resource_file_app_day",     columnList = "application_name, upload_day, is_deleted"),
        @Index(name = "idx_resource_file_tracker_id",  columnList = "resource_tracker_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFile extends BaseUUIDEntity {

    /**
     * UUID used as the physical filename on disk (e.g. "550e8400-e29b-41d4-a716-446655440000.jpg")
     */
    @Column(name = "file_uuid", nullable = false, unique = true, length = 64)
    private String fileUuid;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    /**
     * Application name derived from the API key (acts as top-level folder).
     */
    @Column(name = "application_name", nullable = false)
    private String applicationName;

    /**
     * Business/resource ID for grouping. Delete all files by this ID when business stops service.
     */
    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    /**
     * Upload date in yyyy-MM-dd format (second-level folder).
     */
    @Column(name = "upload_day", nullable = false, length = 10)
    private String uploadDay;

    /**
     * Relative folder path: applicationName/uploadDay/images/ or applicationName/uploadDay/documents/
     */
    @Column(name = "folder_path", nullable = false)
    private String folderPath;

    /**
     * Full relative file path: folderPath + fileUuid + extension
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FileStatus status = FileStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    /**
     * Foreign key to the ResourceTracker record for this (applicationName, resourceId) pair.
     * Allows easy monitoring — join ResourceFile → ResourceTracker to see firstUsedAt / lastUsedAt.
     */
    @Column(name = "resource_tracker_id", columnDefinition = "uuid")
    private UUID resourceTrackerId;
}
