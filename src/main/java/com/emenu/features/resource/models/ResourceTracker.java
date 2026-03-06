package com.emenu.features.resource.models;

import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Tracks every unique (applicationName, resourceId) pair.
 * <p>
 * - firstUsedAt : date of the very first upload under this resourceId
 * - lastUsedAt  : updated to today on EVERY upload (used to detect stale/inactive resourceIds)
 * <p>
 * When lastUsedAt is old you know the resourceId has not been active for a long time
 * and all its files can be safely bulk-deleted.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "resource_trackers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tracker_app_resource",
                        columnNames = {"application_name", "resource_id"}
                )
        },
        indexes = {
                @Index(name = "idx_tracker_app_name",     columnList = "application_name"),
                @Index(name = "idx_tracker_resource_id",  columnList = "resource_id"),
                @Index(name = "idx_tracker_last_used_at", columnList = "last_used_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceTracker extends BaseUUIDEntity {

    @Column(name = "application_name", nullable = false)
    private String applicationName;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    /**
     * Date of the very first upload using this resourceId inside this application.
     */
    @Column(name = "first_used_at", nullable = false, updatable = false)
    private LocalDate firstUsedAt;

    /**
     * Date of the most recent upload using this resourceId.
     * Updated to LocalDate.now() on every upload call.
     */
    @Column(name = "last_used_at", nullable = false)
    private LocalDate lastUsedAt;
}
