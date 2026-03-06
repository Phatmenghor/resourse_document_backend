package com.emenu.features.appkey.mapper;

import com.emenu.features.appkey.dto.response.AppKeyResponse;
import com.emenu.features.appkey.models.AppKey;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppKeyMapper {

    AppKeyResponse toResponse(AppKey appKey);
}
