package com.codeguardian.infrastructure.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration class declaring Kafka topic metadata.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String REVIEW_REQUEST_TOPIC = "review-request";

    /**
     * Declares the main review-request topic.
     * Sets 3 partitions for distributed worker load balancing.
     */
    @Bean
    public NewTopic reviewRequestTopic() {
        return TopicBuilder.name(REVIEW_REQUEST_TOPIC)
                .partitions(3)
                .replicas(1) // Single replica for local development brokers
                .build();
    }
}
