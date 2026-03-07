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
        @Index(name = "idx_resource_file_app_day", columnList = "application_name, upload_day, is_deleted"),
        @Index(name = "idx_resource_file_tracker_id", columnList = "resource_tracker_id"),
        @Index(name = "idx_resource_file_file_path", columnList = "file_path")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFile extends BaseUUIDEntity {

    /** Physical filename on disk (e.g. "07032026_a1b2c3d4.jpg") */
    @Column(name = "file_uuid", nullable = false, unique = true, length = 64)
    private String fileUuid;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    /** Application name derived from the API key (top-level folder). */
    @Column(name = "application_name", nullable = false)
    private String applicationName;

    /** Resource/business ID for grouping. Null if not tied to a specific resource. */
    @Column(name = "resource_id")
    private String resourceId;

    /** Upload date in yyyy-MM-dd format (second-level folder). */
    @Column(name = "upload_day", nullable = false, length = 10)
    private String uploadDay;

    /** Full relative file path: appName/yyyy-MM-dd/ddMMyyyy_xxxxxxxx.ext */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FileStatus status = FileStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "resource_tracker_id", columnDefinition = "uuid")
    private UUID resourceTrackerId;
}
