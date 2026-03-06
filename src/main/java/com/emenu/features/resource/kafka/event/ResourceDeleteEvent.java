package com.emenu.features.resource.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeleteEvent implements Serializable {

    /**
     * List of relative file paths to delete from disk.
     */
    private List<String> filePaths;

    /**
     * Context metadata (for logging / tracing).
     */
    private String resourceId;
    private String applicationName;
}
