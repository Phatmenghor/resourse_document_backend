package com.emenu.features.resource.mapper;

import com.emenu.features.resource.dto.response.ResourceFileResponse;
import com.emenu.features.resource.models.ResourceFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Value;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ResourceFileMapper {

    @Value("${resource.storage.base-url:http://localhost:5000}")
    protected String baseUrl;

    @Mapping(target = "source",     source = "filePath")
    @Mapping(target = "fileType",   expression = "java(resourceFile.getFileType() != null ? resourceFile.getFileType().name() : null)")
    @Mapping(target = "status",     expression = "java(resourceFile.getStatus() != null ? resourceFile.getStatus().name() : null)")
    @Mapping(target = "previewUrl", expression = "java(baseUrl + \"/api/v1/resources/preview?source=\" + resourceFile.getFilePath())")
    public abstract ResourceFileResponse toResponse(ResourceFile resourceFile);
}
