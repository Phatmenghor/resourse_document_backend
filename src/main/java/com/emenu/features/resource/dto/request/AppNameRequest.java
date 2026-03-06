package com.emenu.features.resource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppNameRequest {

    @NotBlank(message = "applicationName is required")
    private String applicationName;
}
