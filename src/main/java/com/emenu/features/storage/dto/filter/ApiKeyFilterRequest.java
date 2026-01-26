package com.emenu.features.storage.dto.filter;

import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApiKeyFilterRequest extends BaseFilterRequest {

    private String name;

    private Boolean isActive;
}
