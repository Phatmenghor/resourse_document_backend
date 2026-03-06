package com.emenu.features.appkey.dto.request;

import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppKeyFilterRequest extends BaseFilterRequest {

    /** Optional: filter by active/inactive status. Null means return all. */
    private Boolean isActive;
}
