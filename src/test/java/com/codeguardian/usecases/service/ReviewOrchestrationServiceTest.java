package com.codeguardian.usecases.service;

import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.domain.model.Review;
import com.codeguardian.domain.model.ReviewComment;
import com.codeguardian.domain.model.ReviewReport;
import com.codeguardian.usecases.port.in.GenerateReviewUseCase;
import com.codeguardian.usecases.port.out.EmbeddingClientPort;
import com.codeguardian.usecases.port.out.GitHubClientPort;
import com.codeguardian.usecases.port.out.ReviewRepositoryPort;
import com.codeguardian.usecases.port.out.VectorStorePort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

class ReviewOrchestrationServiceTest {

    private ReviewOrchestrationService orchestrationService;

    @Mock
    private GenerateReviewUseCase generateReviewUseCase;

    @Mock
    private GitHubClientPort githubClient;

    @Mock
    private ReviewRepositoryPort reviewRepository;

    @Mock
    private EmbeddingClientPort embeddingClient;

    @Mock
    private VectorStorePort vectorStore;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orchestrationService = new ReviewOrchestrationService(
                generateReviewUseCase, githubClient, reviewRepository, embeddingClient, vectorStore);
    }

    @Test
    void orchestrateReview_Success() {
        UUID prId = UUID.randomUUID();
        PullRequest pullRequest = PullRequest.builder()
                .id(prId)
                .githubPrId(101L)
                .prNumber(5)
                .repositoryName("test-repo")
                .owner("test-owner")
                .state("open")
                .build();

        String diffContent = "diff --git a/App.java b/App.java";
        List<ReviewComment> comments = List.of(
                new ReviewComment("App.java", 15, "BUG", "Fix NPE hazard.")
        );
        ReviewReport mockReport = new ReviewReport("Summary review comments text.", 85, comments);

        Mockito.when(embeddingClient.generateEmbedding(any(String.class))).thenReturn(new float[768]);
        Mockito.when(vectorStore.findSimilar(any(float[].class), eq(3))).thenReturn(Collections.emptyList());
        Mockito.when(generateReviewUseCase.execute(eq(diffContent), any(), any())).thenReturn(mockReport);

        orchestrationService.orchestrateReview(pullRequest, diffContent);

        Mockito.verify(embeddingClient, Mockito.times(1)).generateEmbedding(diffContent);
        Mockito.verify(vectorStore, Mockito.times(1)).findSimilar(any(float[].class), eq(3));
        Mockito.verify(generateReviewUseCase, Mockito.times(1)).execute(eq(diffContent), any(), any());
        Mockito.verify(githubClient, Mockito.times(1)).postReview(
                eq("test-owner"),
                eq("test-repo"),
                eq(5),
                contains("Summary review comments text."),
                eq(comments),
                eq(85),
                eq("")
        );
        Mockito.verify(reviewRepository, Mockito.times(1)).save(any(Review.class));
    }

    @Test
    void orchestrateReview_Failure_PersistsFailedReview() {
        UUID prId = UUID.randomUUID();
        PullRequest pullRequest = PullRequest.builder()
                .id(prId)
                .githubPrId(101L)
                .prNumber(5)
                .repositoryName("test-repo")
                .owner("test-owner")
                .state("open")
                .build();

        String diffContent = "diff --git a/App.java b/App.java";
        Mockito.when(embeddingClient.generateEmbedding(any(String.class))).thenReturn(new float[768]);
        Mockito.when(vectorStore.findSimilar(any(float[].class), eq(3))).thenReturn(Collections.emptyList());
        Mockito.when(generateReviewUseCase.execute(eq(diffContent), any(), any()))
                .thenThrow(new RuntimeException("Gemini API connection timed out."));

        Assertions.assertThrows(RuntimeException.class, () -> {
            orchestrationService.orchestrateReview(pullRequest, diffContent);
        });

        // Verify that even if LLM review fails, we persist a FAILED review log to the DB
        Mockito.verify(reviewRepository, Mockito.times(1)).save(any(Review.class));
    }

    @Test
    void orchestrateReview_Bypassed_WhenDiffIsEmpty() {
        PullRequest pr = PullRequest.builder().build();

        orchestrationService.orchestrateReview(pr, "  ");

        Mockito.verifyNoInteractions(generateReviewUseCase);
        Mockito.verifyNoInteractions(githubClient);
        Mockito.verifyNoInteractions(reviewRepository);
        Mockito.verifyNoInteractions(embeddingClient);
        Mockito.verifyNoInteractions(vectorStore);
    }

    @Test
    void fetchPullRequestDiff_DelegatesToGitHubClient() {
        Mockito.when(githubClient.fetchPullRequestDiff("owner", "repo", 5))
                .thenReturn("some-diff");

        String diff = orchestrationService.fetchPullRequestDiff("owner", "repo", 5);

        Assertions.assertEquals("some-diff", diff);
        Mockito.verify(githubClient, Mockito.times(1))
                .fetchPullRequestDiff("owner", "repo", 5);
    }
}
