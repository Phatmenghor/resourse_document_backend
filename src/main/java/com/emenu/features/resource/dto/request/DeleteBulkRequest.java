package com.emenu.features.resource.dto.request;

import lombok.Data;

/**
 * Body for bulk-delete. Provide exactly one field:
 *  - resourceId      → deletes all files for that resource
 *  - applicationName → deletes all files for that application
 *  - apiKey          → looks up the application from the key and deletes all its files
 */
@Data
public class DeleteBulkRequest {

    private String resourceId;
    private String applicationName;
    private String apiKey;
}
