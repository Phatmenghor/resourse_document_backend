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

    private String resourceFileId;
    private String applicationName;
    private String resourceId;
    private String fileUuid;
    private String mimeType;
    private String filePath;
    private String base64Data;
}
