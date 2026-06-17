package com.codeguardian.usecases.service;

import com.codeguardian.usecases.port.out.ReviewRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service to process developer feedback and provide agent acceptance metrics.
 */
@Service
public class FeedbackService {

    private final ReviewRepositoryPort reviewRepositoryPort;

    public FeedbackService(ReviewRepositoryPort reviewRepositoryPort) {
        this.reviewRepositoryPort = reviewRepositoryPort;
    }

    /**
     * Updates the accepted feedback for a given review comment.
     *
     * @param commentId the ID of the comment to update
     * @param accepted true if the suggestion is accepted, false otherwise
     */
    public void submitFeedback(UUID commentId, boolean accepted) {
        if (commentId == null) {
            throw new IllegalArgumentException("Comment ID cannot be null");
        }
        reviewRepositoryPort.updateCommentFeedback(commentId, accepted);
    }

    /**
     * Retrieves acceptance rate metrics for all agent roles.
     *
     * @return Map of agent role names to acceptance percentage (0-100).
     */
    public Map<String, Double> getMetrics() {
        return reviewRepositoryPort.getAcceptanceRates();
    }
}
