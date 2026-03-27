package com.emenu.features.resource.controller;

import com.emenu.features.resource.dto.request.ResourceTrackerFilterRequest;
import com.emenu.features.resource.dto.request.StaleTrackerRequest;
import com.emenu.features.resource.dto.response.ResourceTrackerResponse;
import com.emenu.features.resource.service.ResourceTrackerService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resource-trackers")
@RequiredArgsConstructor
public class ResourceTrackerController {

    private final ResourceTrackerService resourceTrackerService;

    /**
     * Paginated search with optional filters.
     * Supports filtering by applicationName, resourceId, and a general search
     * that matches against both applicationName and resourceId.
     *
     * Body: {
     *   "search": "my-app",         // optional: searches applicationName & resourceId
     *   "applicationName": "my-app", // optional: exact match
     *   "resourceId": "img-001",     // optional: exact match
     *   "pageNo": 1,
     *   "pageSize": 15,
     *   "sortBy": "createdAt",
     *   "sortDirection": "DESC"
     * }
     */
    @PostMapping("/get-all")
    public ResponseEntity<ApiResponse<PaginationResponse<ResourceTrackerResponse>>> search(
            @Valid @RequestBody ResourceTrackerFilterRequest request) {
        PaginationResponse<ResourceTrackerResponse> response = resourceTrackerService.search(request);
        return ResponseEntity.ok(ApiResponse.success("Resource trackers retrieved", response));
    }

    /**
     * List stale resourceIds — those whose lastUsedAt is before the given date.
     * Use this to identify inactive resourceIds that can be cleaned up.
     *
     * Body: { "before": "2026-01-01", "applicationName": "my-app" }
     *       applicationName is optional — omit to search across all applications.
     */
    @PostMapping("/stale")
    public ResponseEntity<ApiResponse<List<ResourceTrackerResponse>>> listStale(
            @Valid @RequestBody StaleTrackerRequest request) {
        List<ResourceTrackerResponse> response = resourceTrackerService.listStale(
                request.getApplicationName(), request.getBefore());
        return ResponseEntity.ok(ApiResponse.success("Stale resource trackers retrieved", response));
    }
}
