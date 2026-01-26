package com.emenu.features.storage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageStatsResponse {

    private UUID apiKeyId;

    private String apiKeyName;

    private Long totalFiles;

    private Long totalStorageBytes;

    private String totalStorageFormatted;

    private Long maxStorageBytes;

    private String maxStorageFormatted;

    private Double usedPercentage;

    private Long totalUploads;

    private Long totalDownloads;

    private Long uploadsToday;

    private Long downloadsToday;

    private Long uploadBytesToday;

    private String uploadBytesTodayFormatted;
}
