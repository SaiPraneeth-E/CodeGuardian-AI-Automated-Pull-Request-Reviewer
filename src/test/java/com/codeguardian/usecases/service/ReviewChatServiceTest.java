package com.codeguardian.usecases.service;

import com.codeguardian.domain.model.CodeSnippet;
import com.codeguardian.domain.model.Review;
import com.codeguardian.domain.model.ReviewComment;
import com.codeguardian.domain.model.ReviewStatus;
import com.codeguardian.usecases.port.out.EmbeddingClientPort;
import com.codeguardian.usecases.port.out.GeminiClientPort;
import com.codeguardian.usecases.port.out.PullRequestRepositoryPort;
import com.codeguardian.usecases.port.out.ReviewRepositoryPort;
import com.codeguardian.usecases.port.out.VectorStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewChatServiceTest {

    private ReviewRepositoryPort reviewRepository;
    private PullRequestRepositoryPort pullRequestRepository;
    private GeminiClientPort geminiClient;
    private EmbeddingClientPort embeddingClient;
    private VectorStorePort vectorStore;
    private ReviewChatService reviewChatService;

    @BeforeEach
    void setUp() {
        reviewRepository = Mockito.mock(ReviewRepositoryPort.class);
        pullRequestRepository = Mockito.mock(PullRequestRepositoryPort.class);
        geminiClient = Mockito.mock(GeminiClientPort.class);
        embeddingClient = Mockito.mock(EmbeddingClientPort.class);
        vectorStore = Mockito.mock(VectorStorePort.class);

        reviewChatService = new ReviewChatService(
                reviewRepository,
                pullRequestRepository,
                geminiClient,
                embeddingClient,
                vectorStore
        );
    }

    @Test
    void chatAboutReview_Success_WithRAG() {
        UUID reviewId = UUID.randomUUID();
        UUID prId = UUID.randomUUID();

        ReviewComment comment = new ReviewComment("src/App.java", 12, "SECURITY", "Avoid hardcoded passwords.");
        Review review = new Review(reviewId, prId, ReviewStatus.COMPLETED, "PR Review Summary", 75, Instant.now(), Instant.now(), List.of(comment));

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // RAG Setup
        float[] mockEmbedding = new float[]{0.1f, 0.2f};
        when(embeddingClient.generateEmbedding(anyString())).thenReturn(mockEmbedding);
        CodeSnippet snippet = new CodeSnippet("src/App.java", 1, 20, "String password = \"12345\";");
        when(vectorStore.findSimilar(mockEmbedding, 3)).thenReturn(List.of(snippet));

        when(geminiClient.generateContent(anyString(), anyString())).thenReturn("This is Gemini response explanation.");

        String response = reviewChatService.chatAboutReview(reviewId, "Why is this hardcoded password flagged?");

        assertEquals("This is Gemini response explanation.", response);
        verify(geminiClient).generateContent(contains("Avoid hardcoded passwords."), eq("Why is this hardcoded password flagged?"));
        verify(geminiClient).generateContent(contains("String password = \"12345\";"), eq("Why is this hardcoded password flagged?"));
    }

    @Test
    void chatAboutReview_ReviewNotFound_ThrowsException() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                reviewChatService.chatAboutReview(reviewId, "Hello assistant"));
    }
}
