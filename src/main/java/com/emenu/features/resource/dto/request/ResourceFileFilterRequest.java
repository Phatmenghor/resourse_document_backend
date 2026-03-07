package com.emenu.features.resource.dto.request;

import com.emenu.enums.resource.FileStatus;
import com.emenu.enums.resource.FileType;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceFileFilterRequest extends BaseFilterRequest {

    /** Optional: filter by exact application name. Null means all applications. */
    private String applicationName;

    /** Optional: filter by exact resourceId. Null means all resources. */
    private String resourceId;

    /** Optional: filter by file type (IMAGE, DOCUMENT). Null means all types. */
    private FileType fileType;

    /** Optional: filter by file status (PENDING, PROCESSING, COMPLETED, FAILED). Null means all statuses. */
    private FileStatus status;
}
