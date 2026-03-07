package com.emenu.features.resource.dto.response;

import com.emenu.enums.resource.FileStatus;
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
    private String resourceId;
    private String previewUrl;
    private FileStatus status;
    private LocalDateTime createdAt;
}
