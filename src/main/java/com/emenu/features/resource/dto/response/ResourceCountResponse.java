package com.emenu.features.resource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCountResponse {

    private long totalFiles;
    private String resourceId;
    private String applicationName;
}
