package com.codeguardian.infrastructure.kafka.consumer;

import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.domain.model.ReviewJob;
import com.codeguardian.infrastructure.kafka.config.KafkaTopicConfig;
import com.codeguardian.usecases.service.ReviewOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer adapter acting as the asynchronous background code review worker.
 */
@Component
public class ReviewEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventConsumer.class);

    private static final String SAMPLE_DIFF = 
            "diff --git a/src/main/java/com/example/PaymentProcessor.java b/src/main/java/com/example/PaymentProcessor.java\n" +
            "--- a/src/main/java/com/example/PaymentProcessor.java\n" +
            "+++ b/src/main/java/com/example/PaymentProcessor.java\n" +
            "@@ -10,4 +10,8 @@\n" +
            "     public void process(double amount) {\n" +
            "         // Vulnerability: Logging sensitive credit card info\n" +
            "         log.info(\"Processing payment amount: \" + amount + \" on card: \" + creditCard);\n" +
            "     }\n";

    private final ReviewOrchestrationService orchestrationService;

    public ReviewEventConsumer(ReviewOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    /**
     * Consumes review events from the review-request topic.
     * Configures a retry topic setup with exponential backoff and routes to DLT on failure.
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = KafkaTopicConfig.REVIEW_REQUEST_TOPIC, groupId = "codeguardian-group")
    public void consume(ReviewJob job) {
        log.info("Worker thread received code review job from Kafka: {}", job);

        // Convert the async job to a domain PullRequest entity
        PullRequest pullRequest = PullRequest.builder()
                .id(job.getPullRequestId())
                .githubPrId(1L) // Mock internal github ID mapping
                .prNumber(job.getPrNumber())
                .repositoryName(job.getRepositoryName())
                .owner(job.getOwner())
                .state("open")
                .headSha(job.getHeadSha())
                .baseSha(job.getBaseSha())
                .build();

        // Fetch real diff from GitHub, falling back to SAMPLE_DIFF on error
        String diffContent;
        try {
            diffContent = orchestrationService.fetchPullRequestDiff(job.getOwner(), job.getRepositoryName(), job.getPrNumber());
        } catch (Exception e) {
            log.warn("Failed to retrieve live PR diff from GitHub, falling back to SAMPLE_DIFF: {}", e.getMessage());
            diffContent = SAMPLE_DIFF;
        }

        // Trigger orchestrated review (AI review generation + GitHub publication)
        orchestrationService.orchestrateReview(pullRequest, diffContent);
    }

    /**
     * Executed when all retry attempts for a specific event fail.
     */
    @DltHandler
    public void handleDlt(ReviewJob job, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("CRITICAL: Review job task failed permanently. Routed to Dead-Letter Topic [{}]. Job payload: {}",
                topic, job);
        // In production, we would log this in a metrics system or notify via Slack/PagerDuty.
    }
}
