package com.emenu.features.resource.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUploadEvent implements Serializable {

    private String resourceFileId;   // UUID of the ResourceFile record
    private String applicationName;
    private String resourceId;
    private String fileUuid;         // physical filename (UUID + extension)
    private String folderPath;       // relative folder path on disk
    private String mimeType;
    private String originalFileName;
    private String base64Data;       // raw base64 (data URI prefix already stripped)

    /**
     * Full relative file path: folderPath + physicalFileName (convenience field).
     */
    private String filePath;
}
