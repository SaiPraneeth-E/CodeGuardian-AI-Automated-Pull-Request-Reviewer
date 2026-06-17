package com.codeguardian.infrastructure.kafka.producer;

import com.codeguardian.domain.model.ReviewJob;
import com.codeguardian.infrastructure.kafka.config.KafkaTopicConfig;
import com.codeguardian.usecases.port.out.ReviewEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Event publisher adapter sending pull request review tasks to Apache Kafka.
 */
@Component
public class ReviewEventPublisher implements ReviewEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventPublisher.class);
    private final KafkaTemplate<String, ReviewJob> kafkaTemplate;

    public ReviewEventPublisher(KafkaTemplate<String, ReviewJob> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(ReviewJob job) {
        log.info("Publishing async code review task for repository {}/{} PR #{} to broker.",
                job.getOwner(), job.getRepositoryName(), job.getPrNumber());

        // System Design constraint: Using repo + PR number as key guarantees message ordering.
        // All events for a specific PR are routed to the same partition, preventing race conditions.
        String messageKey = job.getRepositoryName() + "-" + job.getPrNumber();

        kafkaTemplate.send(KafkaTopicConfig.REVIEW_REQUEST_TOPIC, messageKey, job)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Review event published successfully. Partition: {}, Offset: {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish review event to Kafka broker: {}", ex.getMessage());
                    }
                });
    }
}
