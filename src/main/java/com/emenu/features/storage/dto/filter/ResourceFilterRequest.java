package com.emenu.features.storage.dto.filter;

import com.emenu.enums.storage.ResourceType;
import com.emenu.enums.storage.StorageStatus;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResourceFilterRequest extends BaseFilterRequest {

    private ResourceType resourceType;

    private StorageStatus status;

    private String filename;
}
