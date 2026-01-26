package com.emenu.features.storage.repository;

import com.emenu.enums.storage.ResourceType;
import com.emenu.enums.storage.StorageStatus;
import com.emenu.features.storage.models.ResourceFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceFileRepository extends JpaRepository<ResourceFile, UUID> {

    Optional<ResourceFile> findByIdAndIsDeletedFalse(UUID id);

    Optional<ResourceFile> findByStoredFilenameAndIsDeletedFalse(String storedFilename);

    @Query("SELECT r FROM ResourceFile r WHERE r.apiKey.id = :apiKeyId AND r.isDeleted = false")
    Page<ResourceFile> findByApiKeyId(@Param("apiKeyId") UUID apiKeyId, Pageable pageable);

    @Query("SELECT r FROM ResourceFile r WHERE r.apiKey.id = :apiKeyId AND r.isDeleted = false " +
            "AND (:resourceType IS NULL OR r.resourceType = :resourceType) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:filename IS NULL OR LOWER(r.originalFilename) LIKE LOWER(CONCAT('%', :filename, '%')))")
    Page<ResourceFile> searchResources(
            @Param("apiKeyId") UUID apiKeyId,
            @Param("resourceType") ResourceType resourceType,
            @Param("status") StorageStatus status,
            @Param("filename") String filename,
            Pageable pageable);

    @Query("SELECT r FROM ResourceFile r WHERE r.isTemporary = true " +
            "AND r.expiresAt < :now AND r.status = 'ACTIVE' AND r.isDeleted = false")
    List<ResourceFile> findExpiredTemporaryFiles(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(r) FROM ResourceFile r WHERE r.apiKey.id = :apiKeyId AND r.isDeleted = false AND r.status = 'ACTIVE'")
    Long countActiveFilesByApiKey(@Param("apiKeyId") UUID apiKeyId);

    @Query("SELECT COALESCE(SUM(r.fileSizeBytes), 0) FROM ResourceFile r WHERE r.apiKey.id = :apiKeyId AND r.isDeleted = false AND r.status = 'ACTIVE'")
    Long sumFileSizeByApiKey(@Param("apiKeyId") UUID apiKeyId);

    @Modifying
    @Query("UPDATE ResourceFile r SET r.status = :status WHERE r.id IN :ids")
    int updateStatusByIds(@Param("ids") List<UUID> ids, @Param("status") StorageStatus status);

    @Query("SELECT r FROM ResourceFile r WHERE r.storagePath LIKE :pathPrefix% AND r.isDeleted = false")
    List<ResourceFile> findByStoragePathPrefix(@Param("pathPrefix") String pathPrefix);

    boolean existsByStoredFilenameAndIsDeletedFalse(String storedFilename);
}
