package com.emenu.features.resource.controller;

import com.emenu.features.resource.dto.response.ResourceTrackerResponse;
import com.emenu.features.resource.service.ResourceTrackerService;
import com.emenu.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/resource-trackers")
@RequiredArgsConstructor
public class ResourceTrackerController {

    private final ResourceTrackerService resourceTrackerService;

    /**
     * List all resourceIds tracked under a given application.
     * Shows firstUsedAt and lastUsedAt for each resourceId.
     */
    @GetMapping("/by-app/{applicationName}")
    public ResponseEntity<ApiResponse<List<ResourceTrackerResponse>>> listByApp(
            @PathVariable String applicationName) {
        List<ResourceTrackerResponse> response = resourceTrackerService.listByApplicationName(applicationName);
        return ResponseEntity.ok(ApiResponse.success("Resource trackers retrieved", response));
    }

    /**
     * List stale resourceIds — those whose lastUsedAt is before the given date.
     * Use this to identify which resourceIds have been inactive and can be cleaned up.
     *
     * @param before          Only return resourceIds not used since this date (yyyy-MM-dd)
     * @param applicationName (optional) Filter by a specific application; omit to search all apps
     */
    @GetMapping("/stale")
    public ResponseEntity<ApiResponse<List<ResourceTrackerResponse>>> listStale(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before,
            @RequestParam(required = false) String applicationName) {
        List<ResourceTrackerResponse> response = resourceTrackerService.listStale(applicationName, before);
        return ResponseEntity.ok(ApiResponse.success("Stale resource trackers retrieved", response));
    }
}
