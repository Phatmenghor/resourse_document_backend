package com.emenu.features.appkey.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AppKeyCreateRequest {

    @NotBlank(message = "Application name is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Application name can only contain letters, numbers, hyphens, and underscores")
    private String applicationName;

    private String description;
}
