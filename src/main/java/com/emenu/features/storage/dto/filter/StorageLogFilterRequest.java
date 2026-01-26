package com.emenu.features.storage.dto.filter;

import com.emenu.enums.storage.StorageLogAction;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorageLogFilterRequest extends BaseFilterRequest {

    private StorageLogAction action;

    private Boolean isSuccess;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
