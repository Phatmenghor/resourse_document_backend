package com.emenu.features.storage.tasks;

import com.emenu.features.storage.service.ResourceStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageCleanupScheduler {

    private final ResourceStorageService resourceStorageService;

    /**
     * Clean up expired temporary files every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredFiles() {
        log.info("Starting scheduled cleanup of expired temporary files");
        try {
            int cleanedCount = resourceStorageService.cleanupExpiredFiles();
            log.info("Scheduled cleanup completed. {} expired files cleaned up", cleanedCount);
        } catch (Exception e) {
            log.error("Error during scheduled cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up expired files at startup (5 minutes after application starts)
     */
    @Scheduled(initialDelay = 300000, fixedDelay = Long.MAX_VALUE) // Run once 5 minutes after startup
    public void cleanupExpiredFilesOnStartup() {
        log.info("Starting startup cleanup of expired temporary files");
        try {
            int cleanedCount = resourceStorageService.cleanupExpiredFiles();
            log.info("Startup cleanup completed. {} expired files cleaned up", cleanedCount);
        } catch (Exception e) {
            log.error("Error during startup cleanup: {}", e.getMessage(), e);
        }
    }
}
