package com.emenu.features.resource.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ResourceFileIdRequest {

    @NotNull(message = "id is required")
    private UUID id;
}
