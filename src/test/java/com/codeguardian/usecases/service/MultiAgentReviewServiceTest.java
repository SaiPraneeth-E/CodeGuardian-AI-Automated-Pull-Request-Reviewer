package com.codeguardian.usecases.service;

import com.codeguardian.domain.model.ReviewAgentRole;
import com.codeguardian.domain.model.ReviewComment;
import com.codeguardian.domain.model.ReviewReport;
import com.codeguardian.usecases.port.out.GeminiClientPort;
import com.codeguardian.usecases.port.out.ReviewRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MultiAgentReviewServiceTest {

    private GeminiClientPort geminiClientPort;
    private ReviewRepositoryPort reviewRepositoryPort;
    private MultiAgentReviewService reviewService;

    @BeforeEach
    void setUp() {
        geminiClientPort = Mockito.mock(GeminiClientPort.class);
        reviewRepositoryPort = Mockito.mock(ReviewRepositoryPort.class);
        reviewService = new MultiAgentReviewService(geminiClientPort, reviewRepositoryPort);
    }

    @Test
    void execute_dispatchesToAllAgentsAndAggregates() {
        // Arrange
        String diffContent = "+ System.out.println(\"test\");";

        ReviewComment securityComment = new ReviewComment("Test.java", 1, "SECURITY", "Insecure sysout");
        ReviewReport securityReport = new ReviewReport("Security issues found", 80, List.of(securityComment));
        
        ReviewReport perfReport = new ReviewReport("Performance issues found", 90, List.of());
        ReviewReport testReport = new ReviewReport("Testing issues found", 85, List.of());
        ReviewReport archReport = new ReviewReport("Arch issues found", 95, List.of());

        when(geminiClientPort.generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.SECURITY))).thenReturn(securityReport);
        when(geminiClientPort.generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.PERFORMANCE))).thenReturn(perfReport);
        when(geminiClientPort.generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.TESTING))).thenReturn(testReport);
        when(geminiClientPort.generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.ARCHITECTURE))).thenReturn(archReport);

        when(geminiClientPort.summarizeReviews(anyList())).thenReturn("Final aggregated summary.");

        // Act
        ReviewReport result = reviewService.execute(diffContent);

        // Assert
        assertEquals("Final aggregated summary.", result.getSummary());
        // Avg of 80, 90, 85, 95 is 87
        assertEquals(87, result.getOverallScore());
        assertEquals(1, result.getComments().size());
        assertEquals("SECURITY", result.getComments().get(0).getCategory());

        verify(geminiClientPort, times(1)).generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.SECURITY));
        verify(geminiClientPort, times(1)).generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.PERFORMANCE));
        verify(geminiClientPort, times(1)).generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.TESTING));
        verify(geminiClientPort, times(1)).generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.ARCHITECTURE));
        verify(geminiClientPort, times(1)).summarizeReviews(anyList());
    }

    @Test
    void execute_injectsFeedbackContextSuccessfully() {
        // Arrange
        String diffContent = "+ System.out.println(\"test\");";
        String repoName = "owner/test-repo";
        
        ReviewComment pastComment = new ReviewComment("Test.java", 1, "SECURITY", "Insecure sysout");
        pastComment.setAccepted(false);
        when(reviewRepositoryPort.getRecentFeedback(eq(repoName), eq(5))).thenReturn(List.of(pastComment));

        ReviewReport securityReport = new ReviewReport("Security issues found", 80, List.of(pastComment));
        when(geminiClientPort.generateReview(eq(diffContent), any(), contains("Historical Developer Feedback"), eq(ReviewAgentRole.SECURITY))).thenReturn(securityReport);
        when(geminiClientPort.generateReview(eq(diffContent), any(), contains("Historical Developer Feedback"), eq(ReviewAgentRole.PERFORMANCE))).thenReturn(new ReviewReport("Perf", 100, List.of()));
        when(geminiClientPort.generateReview(eq(diffContent), any(), contains("Historical Developer Feedback"), eq(ReviewAgentRole.TESTING))).thenReturn(new ReviewReport("Test", 100, List.of()));
        when(geminiClientPort.generateReview(eq(diffContent), any(), contains("Historical Developer Feedback"), eq(ReviewAgentRole.ARCHITECTURE))).thenReturn(new ReviewReport("Arch", 100, List.of()));
        when(geminiClientPort.summarizeReviews(anyList())).thenReturn("Summary");

        // Act
        reviewService.execute(diffContent, null, repoName);

        // Assert
        verify(reviewRepositoryPort, times(1)).getRecentFeedback(repoName, 5);
        verify(geminiClientPort, times(1)).generateReview(eq(diffContent), any(), contains("Insecure sysout"), eq(ReviewAgentRole.SECURITY));
    }

    @Test
    void execute_handlesAgentFailureGracefully() {
        // Arrange
        String diffContent = "+ System.out.println(\"test\");";

        ReviewReport perfReport = new ReviewReport("Performance issues found", 90, List.of());
        ReviewReport testReport = new ReviewReport("Testing issues found", 80, List.of());
        ReviewReport archReport = new ReviewReport("Arch issues found", 100, List.of());

        // Security agent fails
        when(geminiClientPort.generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.SECURITY)))
                .thenThrow(new RuntimeException("API Rate Limit Exceeded"));
        
        when(geminiClientPort.generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.PERFORMANCE))).thenReturn(perfReport);
        when(geminiClientPort.generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.TESTING))).thenReturn(testReport);
        when(geminiClientPort.generateReview(eq(diffContent), any(), any(), eq(ReviewAgentRole.ARCHITECTURE))).thenReturn(archReport);

        when(geminiClientPort.summarizeReviews(anyList())).thenReturn("Final aggregated summary with failures.");

        // Act
        ReviewReport result = reviewService.execute(diffContent);

        // Assert
        assertEquals("Final aggregated summary with failures.", result.getSummary());
        // Avg of 90, 80, 100 is 90
        assertEquals(90, result.getOverallScore());
        assertEquals(0, result.getComments().size());

        verify(geminiClientPort, times(1)).summarizeReviews(anyList());
    }

    @Test
    void execute_throwsExceptionIfDiffIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> reviewService.execute(""));
        assertThrows(IllegalArgumentException.class, () -> reviewService.execute(null));
    }
}
