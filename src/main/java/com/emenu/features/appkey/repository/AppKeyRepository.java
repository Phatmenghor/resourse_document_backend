package com.emenu.features.appkey.repository;

import com.emenu.features.appkey.models.AppKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppKeyRepository extends JpaRepository<AppKey, UUID> {

    Optional<AppKey> findByApiKeyAndIsDeletedFalse(String apiKey);

    Optional<AppKey> findByApiKeyAndIsActiveTrueAndIsDeletedFalse(String apiKey);

    Optional<AppKey> findByApplicationNameAndIsDeletedFalse(String applicationName);

    boolean existsByApplicationNameAndIsDeletedFalse(String applicationName);
}
