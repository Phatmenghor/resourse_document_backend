package com.emenu.features.resource.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ResourceTrackerResponse {

    private UUID id;
    private String applicationName;
    private String resourceId;

    /** Date of the very first upload using this resourceId. */
    private LocalDate firstUsedAt;

    /** Date of the most recent upload using this resourceId. */
    private LocalDate lastUsedAt;
}
