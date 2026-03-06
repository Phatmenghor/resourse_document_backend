package com.emenu.features.appkey.service.impl;

import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.UnauthorizedException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.appkey.dto.request.AppKeyCreateRequest;
import com.emenu.features.appkey.dto.request.AppKeyUpdateRequest;
import com.emenu.features.appkey.dto.response.AppKeyResponse;
import com.emenu.features.appkey.mapper.AppKeyMapper;
import com.emenu.features.appkey.models.AppKey;
import com.emenu.features.appkey.repository.AppKeyRepository;
import com.emenu.features.appkey.service.AppKeyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppKeyServiceImpl implements AppKeyService {

    private final AppKeyRepository appKeyRepository;
    private final AppKeyMapper appKeyMapper;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public AppKeyResponse createAppKey(AppKeyCreateRequest request) {
        if (appKeyRepository.existsByApplicationNameAndIsDeletedFalse(request.getApplicationName())) {
            throw new ValidationException("Application name '" + request.getApplicationName() + "' already exists");
        }

        AppKey appKey = new AppKey();
        appKey.setApplicationName(request.getApplicationName());
        appKey.setApiKey(generateSecureApiKey());
        appKey.setDescription(request.getDescription());
        appKey.setIsActive(true);

        AppKey saved = appKeyRepository.save(appKey);
        log.info("Created app key for application: {}", saved.getApplicationName());
        return appKeyMapper.toResponse(saved);
    }

    @Override
    public AppKeyResponse getAppKeyById(UUID id) {
        AppKey appKey = findActiveById(id);
        return appKeyMapper.toResponse(appKey);
    }

    @Override
    public List<AppKeyResponse> getAllAppKeys() {
        return appKeyRepository.findAll().stream()
                .filter(k -> !k.getIsDeleted())
                .map(appKeyMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AppKeyResponse updateAppKey(UUID id, AppKeyUpdateRequest request) {
        AppKey appKey = findActiveById(id);

        if (request.getDescription() != null) {
            appKey.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            appKey.setIsActive(request.getIsActive());
        }

        AppKey saved = appKeyRepository.save(appKey);
        log.info("Updated app key id: {}", id);
        return appKeyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAppKey(UUID id) {
        AppKey appKey = findActiveById(id);
        appKey.softDelete();
        appKeyRepository.save(appKey);
        log.info("Soft deleted app key id: {}", id);
    }

    @Override
    @Transactional
    public AppKeyResponse regenerateApiKey(UUID id) {
        AppKey appKey = findActiveById(id);
        appKey.setApiKey(generateSecureApiKey());
        AppKey saved = appKeyRepository.save(appKey);
        log.info("Regenerated api key for application: {}", saved.getApplicationName());
        return appKeyMapper.toResponse(saved);
    }

    @Override
    public AppKey validateAndGetAppKey(String apiKey) {
        return appKeyRepository.findByApiKeyAndIsActiveTrueAndIsDeletedFalse(apiKey)
                .orElseThrow(() -> new UnauthorizedException("Invalid or inactive API key"));
    }

    // ───────────────────────── helpers ─────────────────────────

    private AppKey findActiveById(UUID id) {
        return appKeyRepository.findById(id)
                .filter(k -> !k.getIsDeleted())
                .orElseThrow(() -> new NotFoundException("App key not found with id: " + id));
    }

    private String generateSecureApiKey() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
