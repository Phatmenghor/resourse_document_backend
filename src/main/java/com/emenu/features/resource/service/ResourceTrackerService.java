package com.emenu.features.resource.service;

import com.emenu.features.resource.dto.request.ResourceTrackerFilterRequest;
import com.emenu.features.resource.dto.response.ResourceTrackerResponse;
import com.emenu.shared.dto.PaginationResponse;

import java.time.LocalDate;
import java.util.List;

public interface ResourceTrackerService {

    /**
     * Called on every upload.
     * - If (applicationName, resourceId) is new → create record with firstUsedAt = lastUsedAt = today.
     * - If it already exists → update lastUsedAt = today.
     *
     * @return the UUID of the ResourceTracker record (for linking from ResourceFile)
     */
    java.util.UUID upsert(String applicationName, String resourceId);

    /** Paginated search with optional filters: search, applicationName, resourceId. */
    PaginationResponse<ResourceTrackerResponse> search(ResourceTrackerFilterRequest request);

    /**
     * Return resourceIds whose lastUsedAt is strictly before {@code before}.
     * If applicationName is null, search across all applications.
     */
    List<ResourceTrackerResponse> listStale(String applicationName, LocalDate before);
}
