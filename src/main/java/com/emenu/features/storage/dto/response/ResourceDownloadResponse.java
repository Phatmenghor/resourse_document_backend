package com.emenu.features.storage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceDownloadResponse {

    private String filename;

    private String mimeType;

    private byte[] data;

    private Long fileSizeBytes;
}
