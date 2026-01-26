package com.emenu.features.storage.mapper;

import com.emenu.features.storage.dto.response.ResourceFileResponse;
import com.emenu.features.storage.dto.response.ResourceUploadResponse;
import com.emenu.features.storage.models.ResourceFile;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ResourceFileMapper {

    @Autowired
    protected PaginationMapper paginationMapper;

    @Value("${app.storage.base-url:http://localhost:5000}")
    protected String baseUrl;

    @Mapping(target = "apiKeyId", source = "apiKey.id")
    @Mapping(target = "apiKeyName", source = "apiKey.name")
    @Mapping(target = "fileSizeFormatted", expression = "java(formatBytes(resourceFile.getFileSizeBytes()))")
    @Mapping(target = "downloadUrl", expression = "java(generateDownloadUrl(resourceFile))")
    public abstract ResourceFileResponse toResponse(ResourceFile resourceFile);

    @Mapping(target = "fileSizeFormatted", expression = "java(formatBytes(resourceFile.getFileSizeBytes()))")
    @Mapping(target = "downloadUrl", expression = "java(generateDownloadUrl(resourceFile))")
    public abstract ResourceUploadResponse toUploadResponse(ResourceFile resourceFile);

    public abstract List<ResourceFileResponse> toResponseList(List<ResourceFile> resourceFiles);

    public PaginationResponse<ResourceFileResponse> toPaginationResponse(Page<ResourceFile> page) {
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

    protected String generateDownloadUrl(ResourceFile resourceFile) {
        if (resourceFile == null || resourceFile.getId() == null) {
            return null;
        }
        return baseUrl + "/api/v1/storage/resources/" + resourceFile.getId() + "/download";
    }
}
