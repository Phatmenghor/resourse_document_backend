package com.emenu.features.resource.dto.response;

import com.emenu.enums.resource.FileStatus;
import com.emenu.enums.resource.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFileResponse {

    private UUID id;
    private String fileUuid;
    private String originalFileName;
    private String mimeType;
    private FileType fileType;
    private String applicationName;
    private String resourceId;
    private String uploadDay;
    private String folderPath;
    private String filePath;
    private Long fileSize;
    private FileStatus status;
    private String previewUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
