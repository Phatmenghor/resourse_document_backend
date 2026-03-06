package com.emenu.features.resource.kafka.consumer;

import com.emenu.enums.resource.FileStatus;
import com.emenu.features.resource.kafka.event.ResourceDeleteEvent;
import com.emenu.features.resource.kafka.event.ResourceUploadEvent;
import com.emenu.features.resource.models.ResourceFile;
import com.emenu.features.resource.repository.ResourceFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceFileConsumer {

    private final ResourceFileRepository resourceFileRepository;

    @Value("${resource.storage.base-path:/app/storage}")
    private String storagePath;

    // ─────────────────────── UPLOAD CONSUMER ───────────────────────

    @KafkaListener(
            topics = "${resource.kafka.topics.upload:resource-file-upload}",
            groupId = "${resource.kafka.consumer.group-id:resource-file-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUploadEvent(ResourceUploadEvent event, Acknowledgment ack) {
        log.info("Processing upload event for file: {} | resourceId: {}",
                event.getFileUuid(), event.getResourceId());

        Optional<ResourceFile> optFile = resourceFileRepository.findById(
                UUID.fromString(event.getResourceFileId()));

        if (optFile.isEmpty()) {
            log.warn("ResourceFile record not found for id: {} – skipping", event.getResourceFileId());
            ack.acknowledge();
            return;
        }

        ResourceFile resourceFile = optFile.get();
        resourceFile.setStatus(FileStatus.PROCESSING);
        resourceFileRepository.save(resourceFile);

        try {
            Path folderAbsPath = Paths.get(storagePath, event.getFolderPath());
            Files.createDirectories(folderAbsPath);

            byte[] fileBytes = Base64.getDecoder().decode(event.getBase64Data());

            Path fileAbsPath = Paths.get(storagePath, event.getFilePath());
            Files.write(fileAbsPath, fileBytes);

            resourceFile.setFileSize((long) fileBytes.length);
            resourceFile.setStatus(FileStatus.COMPLETED);
            resourceFileRepository.save(resourceFile);

            log.info("File saved successfully: {} ({} bytes)", event.getFilePath(), fileBytes.length);

        } catch (IOException e) {
            log.error("Failed to save file: {} | error: {}", event.getFileUuid(), e.getMessage());
            resourceFile.setStatus(FileStatus.FAILED);
            resourceFile.setErrorMessage(e.getMessage());
            resourceFileRepository.save(resourceFile);
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 data for file: {} | error: {}", event.getFileUuid(), e.getMessage());
            resourceFile.setStatus(FileStatus.FAILED);
            resourceFile.setErrorMessage("Invalid base64 data: " + e.getMessage());
            resourceFileRepository.save(resourceFile);
        }

        ack.acknowledge();
    }

    // ─────────────────────── DELETE CONSUMER ───────────────────────

    @KafkaListener(
            topics = "${resource.kafka.topics.delete:resource-file-delete}",
            groupId = "${resource.kafka.consumer.group-id:resource-file-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDeleteEvent(ResourceDeleteEvent event, Acknowledgment ack) {
        log.info("Processing delete event for resourceId: {} | files: {}",
                event.getResourceId(), event.getFilePaths().size());

        int deleted = 0;
        for (String relPath : event.getFilePaths()) {
            try {
                Path absPath = Paths.get(storagePath, relPath);
                if (Files.deleteIfExists(absPath)) {
                    deleted++;
                    log.debug("Deleted physical file: {}", relPath);
                } else {
                    log.warn("Physical file not found (already removed?): {}", relPath);
                }
            } catch (IOException e) {
                log.error("Could not delete file: {} | error: {}", relPath, e.getMessage());
            }
        }

        log.info("Delete event completed for resourceId: {} | deleted {}/{} files",
                event.getResourceId(), deleted, event.getFilePaths().size());

        ack.acknowledge();
    }
}
