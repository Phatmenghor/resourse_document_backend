package com.emenu.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

@Configuration
public class KafkaTopicConfig {

    // ─────────────── existing notification topics ───────────────
    @Value("${notification.kafka.topics.telegram:telegram-notifications}")
    private String telegramTopic;

    @Value("${notification.kafka.topics.email:email-notifications}")
    private String emailTopic;

    @Value("${notification.kafka.topics.logs:notification-logs}")
    private String logsTopic;

    // ─────────────── new resource file topics ───────────────────
    @Value("${resource.kafka.topics.upload:resource-file-upload}")
    private String resourceUploadTopic;

    @Value("${resource.kafka.topics.delete:resource-file-delete}")
    private String resourceDeleteTopic;

    @Bean
    public NewTopic telegramNotificationsTopic() {
        return TopicBuilder.name(telegramTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic emailNotificationsTopic() {
        return TopicBuilder.name(emailTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationLogsTopic() {
        return TopicBuilder.name(logsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic resourceFileUploadTopic() {
        // More partitions for parallel processing of large file uploads
        return TopicBuilder.name(resourceUploadTopic).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic resourceFileDeleteTopic() {
        return TopicBuilder.name(resourceDeleteTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public StringJsonMessageConverter jsonMessageConverter() {
        return new StringJsonMessageConverter();
    }
}
