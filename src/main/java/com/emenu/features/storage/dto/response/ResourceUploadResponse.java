package com.emenu.features.storage.dto.response;

import com.emenu.enums.storage.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceUploadResponse {

    private UUID id;

    private String originalFilename;

    private String storedFilename;

    private ResourceType resourceType;

    private String mimeType;

    private Long fileSizeBytes;

    private String fileSizeFormatted;

    private Boolean isTemporary;

    private LocalDateTime expiresAt;

    private String downloadUrl;

    private LocalDateTime createdAt;
}
