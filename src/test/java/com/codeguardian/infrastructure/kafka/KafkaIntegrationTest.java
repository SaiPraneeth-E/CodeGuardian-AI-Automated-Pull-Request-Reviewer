package com.codeguardian.infrastructure.kafka;

import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.domain.model.ReviewJob;
import com.codeguardian.usecases.port.out.ReviewEventPublisherPort;
import com.codeguardian.usecases.service.ReviewOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("local")
@EmbeddedKafka(
        partitions = 1,
        topics = {"review-request"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext
class KafkaIntegrationTest {

    @Autowired
    private ReviewEventPublisherPort publisher;

    @MockBean
    private ReviewOrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        // Reset the shared mock bean before each test run to prevent stub pollution
        Mockito.reset(orchestrationService);
        Mockito.when(orchestrationService.fetchPullRequestDiff(any(), any(), Mockito.anyInt()))
                .thenReturn("mock-diff-content");
    }

    @Test
    void testPublishAndConsume_Success() {
        ReviewJob job = ReviewJob.builder()
                .owner("test-owner")
                .repositoryName("test-repo")
                .prNumber(123)
                .headSha("sha-head")
                .baseSha("sha-base")
                .build();

        publisher.publish(job);

        // Await consumer receiving message and executing orchestrator asynchronously
        await().atMost(8, TimeUnit.SECONDS).untilAsserted(() -> {
            Mockito.verify(orchestrationService, Mockito.times(1))
                    .orchestrateReview(any(PullRequest.class), any(String.class));
        });
    }

    @Test
    void testPublishAndConsume_RetryExhaustionRoutesToDlt() {
        ReviewJob job = ReviewJob.builder()
                .owner("fail-owner")
                .repositoryName("fail-repo")
                .prNumber(999)
                .build();

        // Simulate repeated service failure to test retry limit
        Mockito.doThrow(new RuntimeException("Simulated Gemini API Timeout"))
                .when(orchestrationService)
                .orchestrateReview(any(PullRequest.class), any(String.class));

        publisher.publish(job);

        // Verify that the task is retried exactly 3 times (1 initial + 2 retries) before going to DLT
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Mockito.verify(orchestrationService, Mockito.times(3))
                    .orchestrateReview(any(PullRequest.class), any(String.class));
        });
    }
}
