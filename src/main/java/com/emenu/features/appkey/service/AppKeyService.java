package com.emenu.features.appkey.service;

import com.emenu.features.appkey.dto.request.AppKeyCreateRequest;
import com.emenu.features.appkey.dto.request.AppKeyFilterRequest;
import com.emenu.features.appkey.dto.request.AppKeyUpdateRequest;
import com.emenu.features.appkey.dto.response.AppKeyResponse;
import com.emenu.features.appkey.models.AppKey;
import com.emenu.shared.dto.PaginationResponse;

import java.util.UUID;

public interface AppKeyService {

    AppKeyResponse createAppKey(AppKeyCreateRequest request);

    AppKeyResponse getAppKeyById(UUID id);

    PaginationResponse<AppKeyResponse> searchAppKeys(AppKeyFilterRequest request);

    AppKeyResponse updateAppKey(UUID id, AppKeyUpdateRequest request);

    void deleteAppKey(UUID id);

    AppKeyResponse regenerateApiKey(UUID id);

    /**
     * Validate an API key and return the associated AppKey entity.
     * Throws UnauthorizedException if key is invalid or inactive.
     */
    AppKey validateAndGetAppKey(String apiKey);
}
