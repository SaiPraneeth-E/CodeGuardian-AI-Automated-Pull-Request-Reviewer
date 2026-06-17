package com.codeguardian.usecases.service;

import com.codeguardian.usecases.port.out.ReviewRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {

    private ReviewRepositoryPort reviewRepositoryPort;
    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        reviewRepositoryPort = Mockito.mock(ReviewRepositoryPort.class);
        feedbackService = new FeedbackService(reviewRepositoryPort);
    }

    @Test
    void submitFeedback_Success() {
        UUID commentId = UUID.randomUUID();

        feedbackService.submitFeedback(commentId, true);

        verify(reviewRepositoryPort, times(1)).updateCommentFeedback(eq(commentId), eq(true));
    }

    @Test
    void submitFeedback_ThrowsIllegalArgumentExceptionForNullId() {
        assertThrows(IllegalArgumentException.class, () -> feedbackService.submitFeedback(null, true));
    }

    @Test
    void getMetrics_ReturnsAcceptanceRates() {
        Map<String, Double> mockMetrics = Map.of(
                "SECURITY", 85.5,
                "PERFORMANCE", 92.0
        );
        when(reviewRepositoryPort.getAcceptanceRates()).thenReturn(mockMetrics);

        Map<String, Double> result = feedbackService.getMetrics();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(85.5, result.get("SECURITY"));
        assertEquals(92.0, result.get("PERFORMANCE"));
    }
}
