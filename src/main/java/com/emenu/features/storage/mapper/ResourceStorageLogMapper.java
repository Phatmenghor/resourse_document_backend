package com.emenu.features.storage.mapper;

import com.emenu.features.storage.dto.response.ResourceStorageLogResponse;
import com.emenu.features.storage.models.ResourceStorageLog;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ResourceStorageLogMapper {

    @Autowired
    protected PaginationMapper paginationMapper;

    @Mapping(target = "apiKeyId", source = "apiKey.id")
    @Mapping(target = "apiKeyName", source = "apiKey.name")
    @Mapping(target = "fileSizeFormatted", expression = "java(formatBytes(log.getFileSizeBytes()))")
    public abstract ResourceStorageLogResponse toResponse(ResourceStorageLog log);

    public abstract List<ResourceStorageLogResponse> toResponseList(List<ResourceStorageLog> logs);

    public PaginationResponse<ResourceStorageLogResponse> toPaginationResponse(Page<ResourceStorageLog> page) {
        return paginationMapper.toPaginationResponse(page, this::toResponseList);
    }

    protected String formatBytes(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }
}
