package com.emenu.features.storage.mapper;

import com.emenu.features.storage.dto.request.CreateStorageApiKeyRequest;
import com.emenu.features.storage.dto.request.UpdateStorageApiKeyRequest;
import com.emenu.features.storage.dto.response.StorageApiKeyCreatedResponse;
import com.emenu.features.storage.dto.response.StorageApiKeyResponse;
import com.emenu.features.storage.models.StorageApiKey;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.mapper.PaginationMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class StorageApiKeyMapper {

    @Autowired
    protected PaginationMapper paginationMapper;

    @Mapping(target = "storageUsedPercentage", expression = "java(calculateStorageUsedPercentage(apiKey))")
    @Mapping(target = "currentStorageFormatted", expression = "java(formatBytes(apiKey.getCurrentStorageBytes()))")
    @Mapping(target = "maxStorageFormatted", expression = "java(formatBytes(apiKey.getMaxStorageMb() * 1024 * 1024))")
    public abstract StorageApiKeyResponse toResponse(StorageApiKey apiKey);

    public abstract StorageApiKeyCreatedResponse toCreatedResponse(StorageApiKey apiKey);

    public abstract List<StorageApiKeyResponse> toResponseList(List<StorageApiKey> apiKeys);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateEntity(UpdateStorageApiKeyRequest request, @MappingTarget StorageApiKey apiKey);

    public StorageApiKey toEntity(CreateStorageApiKeyRequest request) {
        StorageApiKey apiKey = new StorageApiKey();
        apiKey.setName(request.getName());
        apiKey.setDescription(request.getDescription());
        apiKey.setMaxFileSizeMb(request.getMaxFileSizeMb() != null ? request.getMaxFileSizeMb() : 10L);
        apiKey.setMaxStorageMb(request.getMaxStorageMb() != null ? request.getMaxStorageMb() : 1024L);
        apiKey.setAllowedFileTypes(request.getAllowedFileTypes());
        apiKey.setIsActive(true);
        apiKey.setCurrentStorageBytes(0L);
        apiKey.setTotalUploadCount(0L);
        apiKey.setTotalDownloadCount(0L);
        return apiKey;
    }

    public PaginationResponse<StorageApiKeyResponse> toPaginationResponse(Page<StorageApiKey> page) {
        return paginationMapper.toPaginationResponse(page, this::toResponseList);
    }

    protected Double calculateStorageUsedPercentage(StorageApiKey apiKey) {
        if (apiKey.getMaxStorageMb() == null || apiKey.getMaxStorageMb() == 0) {
            return 0.0;
        }
        long maxBytes = apiKey.getMaxStorageMb() * 1024 * 1024;
        long currentBytes = apiKey.getCurrentStorageBytes() != null ? apiKey.getCurrentStorageBytes() : 0L;
        return Math.round((currentBytes * 100.0 / maxBytes) * 100.0) / 100.0;
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
