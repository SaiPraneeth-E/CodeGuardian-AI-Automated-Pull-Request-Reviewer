package com.codeguardian.usecases.service;

import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.domain.model.ReviewJob;
import com.codeguardian.usecases.port.out.PullRequestRepositoryPort;
import com.codeguardian.usecases.port.out.ReviewEventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class ProcessWebhookServiceTest {

    private ProcessWebhookService processWebhookService;

    @Mock
    private PullRequestRepositoryPort repositoryPort;

    @Mock
    private ReviewEventPublisherPort eventPublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processWebhookService = new ProcessWebhookService(repositoryPort, eventPublisher);
    }

    @Test
    void execute_NewPullRequest_SavesAndPublishesSuccessfully() {
        PullRequest prInput = PullRequest.builder()
                .githubPrId(100L)
                .prNumber(1)
                .repositoryName("test-repo")
                .owner("owner")
                .title("Add login feature")
                .state("open")
                .headSha("sha-head")
                .baseSha("sha-base")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        UUID expectedId = UUID.randomUUID();
        PullRequest prSavedResult = prInput.toBuilder().id(expectedId).build();

        Mockito.when(repositoryPort.findByGithubPrId(100L)).thenReturn(Optional.empty());
        Mockito.when(repositoryPort.save(any(PullRequest.class))).thenReturn(prSavedResult);

        PullRequest result = processWebhookService.execute(prInput);

        assertNotNull(result.getId());
        assertEquals(expectedId, result.getId());
        assertEquals(prInput.getGithubPrId(), result.getGithubPrId());
        
        Mockito.verify(repositoryPort, Mockito.times(1)).save(any(PullRequest.class));
        Mockito.verify(eventPublisher, Mockito.times(1)).publish(any(ReviewJob.class));
    }

    @Test
    void execute_ExistingPullRequest_UpdatesAndPublishesSuccessfully() {
        UUID existingId = UUID.randomUUID();
        Instant originalCreatedAt = Instant.now().minusSeconds(1000);

        PullRequest existingPr = PullRequest.builder()
                .id(existingId)
                .githubPrId(100L)
                .prNumber(1)
                .repositoryName("test-repo")
                .owner("owner")
                .title("Add login feature")
                .state("open")
                .headSha("sha-head-old")
                .baseSha("sha-base")
                .createdAt(originalCreatedAt)
                .updatedAt(originalCreatedAt)
                .build();

        PullRequest incomingPrPayload = PullRequest.builder()
                .githubPrId(100L)
                .prNumber(1)
                .repositoryName("test-repo")
                .owner("owner")
                .title("Add login feature (updated)")
                .state("closed")
                .headSha("sha-head-new")
                .baseSha("sha-base")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Mockito.when(repositoryPort.findByGithubPrId(100L)).thenReturn(Optional.of(existingPr));
        Mockito.when(repositoryPort.save(any(PullRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PullRequest result = processWebhookService.execute(incomingPrPayload);

        assertEquals(existingId, result.getId());
        assertEquals(originalCreatedAt, result.getCreatedAt());
        assertEquals("closed", result.getState());
        assertEquals("sha-head-new", result.getHeadSha());
        
        Mockito.verify(repositoryPort, Mockito.times(1)).save(any(PullRequest.class));
        Mockito.verify(eventPublisher, Mockito.times(1)).publish(any(ReviewJob.class));
    }

    @Test
    void execute_InvalidPullRequest_ThrowsException() {
        PullRequest invalidPr = PullRequest.builder()
                .githubPrId(null)
                .prNumber(1)
                .repositoryName("test")
                .owner("owner")
                .state("open")
                .build();

        assertThrows(IllegalArgumentException.class, () -> processWebhookService.execute(invalidPr));
        Mockito.verifyNoInteractions(eventPublisher);
    }
}
