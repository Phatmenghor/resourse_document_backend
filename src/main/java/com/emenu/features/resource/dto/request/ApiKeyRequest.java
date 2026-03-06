package com.emenu.features.resource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiKeyRequest {

    @NotBlank(message = "apiKey is required")
    private String apiKey;
}
