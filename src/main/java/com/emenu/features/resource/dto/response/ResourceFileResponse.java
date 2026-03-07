package com.emenu.features.resource.dto.response;

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
    private String mimeType;
    private String fileType;
    private String status;
    /** Relative file path on disk (e.g. my-app/2026-03-07/07032026_abc123.pdf) */
    private String source;
    private String previewUrl;
    private LocalDateTime createdAt;
}
