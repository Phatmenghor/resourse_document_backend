package com.emenu.features.storage.repository;

import com.emenu.enums.storage.StorageLogAction;
import com.emenu.features.storage.models.ResourceStorageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceStorageLogRepository extends JpaRepository<ResourceStorageLog, UUID> {

    @Query("SELECT l FROM ResourceStorageLog l WHERE l.apiKey.id = :apiKeyId AND l.isDeleted = false " +
            "ORDER BY l.actionTimestamp DESC")
    Page<ResourceStorageLog> findByApiKeyId(@Param("apiKeyId") UUID apiKeyId, Pageable pageable);

    @Query("SELECT l FROM ResourceStorageLog l WHERE l.apiKey.id = :apiKeyId AND l.isDeleted = false " +
            "AND (:action IS NULL OR l.action = :action) " +
            "AND (:isSuccess IS NULL OR l.isSuccess = :isSuccess) " +
            "AND (:startDate IS NULL OR l.actionTimestamp >= :startDate) " +
            "AND (:endDate IS NULL OR l.actionTimestamp <= :endDate) " +
            "ORDER BY l.actionTimestamp DESC")
    Page<ResourceStorageLog> searchLogs(
            @Param("apiKeyId") UUID apiKeyId,
            @Param("action") StorageLogAction action,
            @Param("isSuccess") Boolean isSuccess,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT l FROM ResourceStorageLog l WHERE l.resourceFileId = :resourceFileId AND l.isDeleted = false " +
            "ORDER BY l.actionTimestamp DESC")
    List<ResourceStorageLog> findByResourceFileId(@Param("resourceFileId") UUID resourceFileId);

    @Query("SELECT COUNT(l) FROM ResourceStorageLog l WHERE l.apiKey.id = :apiKeyId " +
            "AND l.action = :action AND l.isDeleted = false")
    Long countByApiKeyIdAndAction(@Param("apiKeyId") UUID apiKeyId, @Param("action") StorageLogAction action);

    @Query("SELECT COUNT(l) FROM ResourceStorageLog l WHERE l.apiKey.id = :apiKeyId " +
            "AND l.action = :action AND l.isSuccess = true AND l.isDeleted = false " +
            "AND l.actionTimestamp >= :startDate AND l.actionTimestamp <= :endDate")
    Long countSuccessfulActionsBetween(
            @Param("apiKeyId") UUID apiKeyId,
            @Param("action") StorageLogAction action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(l.fileSizeBytes), 0) FROM ResourceStorageLog l " +
            "WHERE l.apiKey.id = :apiKeyId AND l.action = 'UPLOAD' AND l.isSuccess = true " +
            "AND l.isDeleted = false AND l.actionTimestamp >= :startDate AND l.actionTimestamp <= :endDate")
    Long sumUploadedBytesBetween(
            @Param("apiKeyId") UUID apiKeyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT l FROM ResourceStorageLog l WHERE l.actionTimestamp < :before AND l.isDeleted = false")
    List<ResourceStorageLog> findOldLogs(@Param("before") LocalDateTime before);
}
