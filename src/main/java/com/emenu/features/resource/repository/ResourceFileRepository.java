package com.emenu.features.resource.repository;

import com.emenu.enums.resource.FileStatus;
import com.emenu.enums.resource.FileType;
import com.emenu.features.resource.models.ResourceFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT f FROM ResourceFile f
            WHERE f.isDeleted = false
              AND (:search IS NULL OR :search = ''
                   OR LOWER(f.resourceId) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(f.applicationName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:applicationName IS NULL OR :applicationName = '' OR f.applicationName = :applicationName)
              AND (:resourceId IS NULL OR :resourceId = '' OR f.resourceId = :resourceId)
              AND (:fileType IS NULL OR f.fileType = :fileType)
              AND (:status IS NULL OR f.status = :status)
            """)
    Page<ResourceFile> search(@Param("search") String search,
                              @Param("applicationName") String applicationName,
                              @Param("resourceId") String resourceId,
                              @Param("fileType") FileType fileType,
                              @Param("status") FileStatus status,
                              Pageable pageable);
}
