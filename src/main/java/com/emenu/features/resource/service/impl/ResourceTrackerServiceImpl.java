package com.emenu.features.resource.service.impl;

import com.emenu.features.resource.dto.response.ResourceTrackerResponse;
import com.emenu.features.resource.models.ResourceTracker;
import com.emenu.features.resource.repository.ResourceTrackerRepository;
import com.emenu.features.resource.service.ResourceTrackerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            // Already tracked — just refresh lastUsedAt
            ResourceTracker tracker = existing.get();
            tracker.setLastUsedAt(today);
            ResourceTracker saved = resourceTrackerRepository.save(tracker);
            log.info("ResourceTracker updated lastUsedAt={} | app={} | resourceId={}", today, applicationName, resourceId);
            return saved.getId();
        } else {
            // First time this resourceId is used under this application
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
    public List<ResourceTrackerResponse> listByApplicationName(String applicationName) {
        return resourceTrackerRepository.findByApplicationName(applicationName)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
