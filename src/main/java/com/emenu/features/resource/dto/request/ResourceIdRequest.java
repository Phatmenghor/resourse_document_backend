package com.emenu.features.resource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResourceIdRequest {

    @NotBlank(message = "resourceId is required")
    private String resourceId;
}
