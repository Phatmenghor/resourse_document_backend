package com.emenu.features.resource.kafka.producer;

import com.emenu.features.resource.kafka.event.ResourceDeleteEvent;
import com.emenu.features.resource.kafka.event.ResourceUploadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceFileProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${resource.kafka.topics.upload:resource-file-upload}")
    private String uploadTopic;

    @Value("${resource.kafka.topics.delete:resource-file-delete}")
    private String deleteTopic;

    public void sendUploadEvent(ResourceUploadEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(uploadTopic, event.getResourceFileId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Upload event sent for file: {} | partition: {} | offset: {}",
                        event.getFileUuid(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send upload event for file: {} | error: {}",
                        event.getFileUuid(), ex.getMessage());
            }
        });
    }

    public void sendDeleteEvent(ResourceDeleteEvent event) {
        String key = event.getResourceId() + "-" + event.getApplicationName();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(deleteTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Delete event sent for resourceId: {} | files: {}",
                        event.getResourceId(), event.getFilePaths().size());
            } else {
                log.error("Failed to send delete event for resourceId: {} | error: {}",
                        event.getResourceId(), ex.getMessage());
            }
        });
    }
}
