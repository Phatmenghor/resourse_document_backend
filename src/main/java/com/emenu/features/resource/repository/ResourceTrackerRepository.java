package com.emenu.features.resource.repository;

import com.emenu.features.resource.models.ResourceTracker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceTrackerRepository extends JpaRepository<ResourceTracker, UUID> {

    Optional<ResourceTracker> findByApplicationNameAndResourceId(String applicationName, String resourceId);

    List<ResourceTracker> findByApplicationName(String applicationName);

    /** All resourceIds across ALL applications whose lastUsedAt is before the given date. */
    List<ResourceTracker> findByLastUsedAtBefore(LocalDate date);

    /** Stale resourceIds within a specific application. */
    List<ResourceTracker> findByApplicationNameAndLastUsedAtBefore(String applicationName, LocalDate date);

    @Query("""
            SELECT t FROM ResourceTracker t
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(t.applicationName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.resourceId) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:applicationName IS NULL OR :applicationName = '' OR t.applicationName = :applicationName)
              AND (:resourceId IS NULL OR :resourceId = '' OR t.resourceId = :resourceId)
            """)
    Page<ResourceTracker> search(@Param("search") String search,
                                 @Param("applicationName") String applicationName,
                                 @Param("resourceId") String resourceId,
                                 Pageable pageable);
}
