package com.emenu.features.resource.repository;

import com.emenu.features.resource.models.ResourceTracker;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
