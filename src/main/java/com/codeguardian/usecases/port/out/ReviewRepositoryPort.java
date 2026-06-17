package com.codeguardian.usecases.port.out;

import com.codeguardian.domain.model.Review;
import com.codeguardian.domain.model.ReviewComment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Review persistence operations.
 */
public interface ReviewRepositoryPort {
    /**
     * Finds a Review by its database ID.
     *
     * @param id the review database UUID
     * @return an Optional of the Review domain model
     */
    Optional<Review> findById(UUID id);
    /**
     * Saves a Review record along with its associated comments.
     *
     * @param review the review domain object to persist
     * @return the persisted review domain object
     */
    Review save(Review review);

    /**
     * Updates the accepted feedback status of a specific review comment.
     *
     * @param commentId the ID of the comment to update
     * @param accepted whether the developer accepted or rejected the suggestion
     */
    void updateCommentFeedback(UUID commentId, boolean accepted);

    /**
     * Retrieves the most recent feedback records for a specific repository.
     *
     * @param repositoryName the name of the repository (e.g. owner/repo)
     * @param limit the maximum number of feedback records to retrieve
     * @return list of recent comments that received feedback
     */
    List<ReviewComment> getRecentFeedback(String repositoryName, int limit);

    /**
     * Calculates the acceptance rate grouped by the AI agent role category.
     *
     * @return a map where the key is the agent role and the value is the acceptance rate percentage (0.0 to 100.0)
     */
    Map<String, Double> getAcceptanceRates();
}
