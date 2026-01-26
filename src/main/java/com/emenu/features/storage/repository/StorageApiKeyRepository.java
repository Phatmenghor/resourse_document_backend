package com.emenu.features.storage.repository;

import com.emenu.features.storage.models.StorageApiKey;
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
public interface StorageApiKeyRepository extends JpaRepository<StorageApiKey, UUID> {

    Optional<StorageApiKey> findByApiKeyAndIsDeletedFalse(String apiKey);

    Optional<StorageApiKey> findByNameAndIsDeletedFalse(String name);

    Optional<StorageApiKey> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByApiKeyAndIsDeletedFalse(String apiKey);

    @Query("SELECT k FROM StorageApiKey k WHERE k.isDeleted = false AND k.isActive = true")
    List<StorageApiKey> findAllActiveKeys();

    @Query("SELECT k FROM StorageApiKey k WHERE k.isDeleted = false " +
            "AND (:name IS NULL OR LOWER(k.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:isActive IS NULL OR k.isActive = :isActive)")
    Page<StorageApiKey> searchApiKeys(
            @Param("name") String name,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    @Query("SELECT k FROM StorageApiKey k WHERE k.apiKeyPrefix = :prefix AND k.isDeleted = false")
    Optional<StorageApiKey> findByApiKeyPrefix(@Param("prefix") String prefix);
}
