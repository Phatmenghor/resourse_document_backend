package com.emenu.features.resource.service.impl;

import com.emenu.features.resource.dto.request.ResourceTrackerFilterRequest;
import com.emenu.features.resource.dto.response.ResourceTrackerResponse;
import com.emenu.features.resource.models.ResourceTracker;
import com.emenu.features.resource.repository.ResourceTrackerRepository;
import com.emenu.features.resource.service.ResourceTrackerService;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.pagination.PaginationUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceTrackerServiceImpl implements ResourceTrackerService {

    private final ResourceTrackerRepository resourceTrackerRepository;

    @Override
    @Transactional
    public java.util.UUID upsert(String applicationName, String resourceId) {
        LocalDate today = LocalDate.now();

        Optional<ResourceTracker> existing =
                resourceTrackerRepository.findByApplicationNameAndResourceId(applicationName, resourceId);

        if (existing.isPresent()) {
            ResourceTracker tracker = existing.get();
            tracker.setLastUsedAt(today);
            ResourceTracker saved = resourceTrackerRepository.save(tracker);
            log.info("ResourceTracker updated lastUsedAt={} | app={} | resourceId={}", today, applicationName, resourceId);
            return saved.getId();
        } else {
            ResourceTracker tracker = new ResourceTracker();
            tracker.setApplicationName(applicationName);
            tracker.setResourceId(resourceId);
            tracker.setFirstUsedAt(today);
            tracker.setLastUsedAt(today);
            ResourceTracker saved = resourceTrackerRepository.save(tracker);
            log.info("ResourceTracker created firstUsedAt={} | app={} | resourceId={}", today, applicationName, resourceId);
            return saved.getId();
        }
    }

    @Override
    public PaginationResponse<ResourceTrackerResponse> search(ResourceTrackerFilterRequest request) {
        Pageable pageable = PaginationUtils.createPageable(
                request.getPageNo(), request.getPageSize(),
                request.getSortBy(), request.getSortDirection());

        String search = request.getSearch() == null ? "" : request.getSearch().trim();
        String applicationName = request.getApplicationName();
        String resourceId = request.getResourceId();

        Page<ResourceTracker> page = resourceTrackerRepository.search(search, applicationName, resourceId, pageable);

        return PaginationResponse.<ResourceTrackerResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Override
    public List<ResourceTrackerResponse> listStale(String applicationName, LocalDate before) {
        List<ResourceTracker> results = (applicationName != null && !applicationName.isBlank())
                ? resourceTrackerRepository.findByApplicationNameAndLastUsedAtBefore(applicationName, before)
                : resourceTrackerRepository.findByLastUsedAtBefore(before);

        return results.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ResourceTrackerResponse toResponse(ResourceTracker t) {
        return ResourceTrackerResponse.builder()
                .id(t.getId())
                .applicationName(t.getApplicationName())
                .resourceId(t.getResourceId())
                .firstUsedAt(t.getFirstUsedAt())
                .lastUsedAt(t.getLastUsedAt())
                .build();
    }
}
