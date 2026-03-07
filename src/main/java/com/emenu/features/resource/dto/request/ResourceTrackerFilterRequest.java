package com.emenu.features.resource.dto.request;

import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceTrackerFilterRequest extends BaseFilterRequest {

    /** Optional: filter by exact application name. Null means all applications. */
    private String applicationName;

    /** Optional: filter by exact resourceId. Null means all resources. */
    private String resourceId;
}
