package com.emenu.features.resource.repository;

import com.emenu.features.resource.models.ResourceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceFileRepository extends JpaRepository<ResourceFile, UUID> {

    Optional<ResourceFile> findByIdAndIsDeletedFalse(UUID id);

    Optional<ResourceFile> findByFilePathAndIsDeletedFalse(String filePath);

    Optional<ResourceFile> findByFileUuidAndIsDeletedFalse(String fileUuid);

    List<ResourceFile> findByResourceIdAndIsDeletedFalse(String resourceId);

    List<ResourceFile> findByApplicationNameAndIsDeletedFalse(String applicationName);

    List<ResourceFile> findByResourceIdAndApplicationNameAndIsDeletedFalse(
            String resourceId, String applicationName);

    @Query("SELECT COUNT(r) FROM ResourceFile r WHERE r.isDeleted = false")
    long countAllActive();

    @Query("SELECT COUNT(r) FROM ResourceFile r WHERE r.resourceId = :resourceId AND r.isDeleted = false")
    long countByResourceId(@Param("resourceId") String resourceId);

    @Query("SELECT COUNT(r) FROM ResourceFile r WHERE r.applicationName = :applicationName AND r.isDeleted = false")
    long countByApplicationName(@Param("applicationName") String applicationName);
}
