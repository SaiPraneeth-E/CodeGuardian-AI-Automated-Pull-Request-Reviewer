package com.codeguardian.adapters.out.persistence;

import com.codeguardian.domain.model.Review;
import com.codeguardian.domain.model.ReviewComment;
import com.codeguardian.usecases.port.out.ReviewRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

/**
 * Persistence Adapter implementing ReviewRepositoryPort for saving review logs and comments.
 */
@Component
public class ReviewPersistenceAdapter implements ReviewRepositoryPort {

    private final SpringDataReviewRepository reviewRepository;
    private final SpringDataPullRequestRepository pullRequestRepository;
    private final SpringDataReviewCommentRepository commentRepository;

    public ReviewPersistenceAdapter(SpringDataReviewRepository reviewRepository,
                                    SpringDataPullRequestRepository pullRequestRepository,
                                    SpringDataReviewCommentRepository commentRepository) {
        this.reviewRepository = reviewRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findById(UUID id) {
        return reviewRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional
    public Review save(Review review) {
        if (review == null) {
            return null;
        }

        // Fetch the corresponding PullRequest JPA Entity from the DB
        PullRequestJpaEntity prEntity = pullRequestRepository.findById(review.getPullRequestId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot save review: PullRequest not found with ID: " + review.getPullRequestId()));

        // Map domain Review to JPA entity
        ReviewJpaEntity reviewEntity = mapToJpaEntity(review, prEntity);

        // Save reviewEntity (this will cascade save comments due to CascadeType.ALL)
        ReviewJpaEntity savedEntity = reviewRepository.save(reviewEntity);

        // Map back to domain model and return
        return mapToDomain(savedEntity);
    }

    private ReviewJpaEntity mapToJpaEntity(Review domain, PullRequestJpaEntity prEntity) {
        UUID reviewId = domain.getId() != null ? domain.getId() : UUID.randomUUID();

        List<ReviewCommentJpaEntity> commentEntities = new ArrayList<>();
        if (domain.getComments() != null) {
            for (ReviewComment comment : domain.getComments()) {
                UUID commentId = comment.getId() != null ? comment.getId() : UUID.randomUUID();
                commentEntities.add(ReviewCommentJpaEntity.builder()
                        .id(commentId)
                        .filePath(comment.getFilePath())
                        .lineNumber(comment.getLineNumber())
                        .agentType(comment.getCategory())
                        .commentText(comment.getCommentText())
                        .createdAt(Instant.now())
                        .accepted(comment.getAccepted())
                        .build());
            }
        }

        Instant createdAt = domain.getCreatedAt() != null ? domain.getCreatedAt() : Instant.now();

        return ReviewJpaEntity.builder()
                .id(reviewId)
                .pullRequest(prEntity)
                .status(domain.getStatus().name())
                .summary(domain.getSummary())
                .overallScore(domain.getOverallScore())
                .createdAt(createdAt)
                .completedAt(domain.getCompletedAt())
                .comments(commentEntities)
                .build();
    }

    private Review mapToDomain(ReviewJpaEntity entity) {
        List<ReviewComment> comments = new ArrayList<>();
        if (entity.getComments() != null) {
            comments = entity.getComments().stream()
                    .map(c -> {
                        ReviewComment rc = new ReviewComment(c.getFilePath(), c.getLineNumber(), c.getAgentType(), c.getCommentText());
                        rc.setId(c.getId());
                        rc.setAccepted(c.getAccepted());
                        return rc;
                    })
                    .collect(Collectors.toList());
        }

        return Review.builder()
                .id(entity.getId())
                .pullRequestId(entity.getPullRequest().getId())
                .status(com.codeguardian.domain.model.ReviewStatus.valueOf(entity.getStatus()))
                .summary(entity.getSummary())
                .overallScore(entity.getOverallScore())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .comments(comments)
                .build();
    }

    @Override
    @Transactional
    public void updateCommentFeedback(UUID commentId, boolean accepted) {
        ReviewCommentJpaEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with ID: " + commentId));
        comment.setAccepted(accepted);
        commentRepository.save(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewComment> getRecentFeedback(String repositoryName, int limit) {
        List<ReviewCommentJpaEntity> entities = commentRepository.findRecentFeedbackByRepository(
                repositoryName, PageRequest.of(0, limit));

        return entities.stream()
                .map(c -> {
                    ReviewComment rc = new ReviewComment(c.getFilePath(), c.getLineNumber(), c.getAgentType(), c.getCommentText());
                    rc.setId(c.getId());
                    rc.setAccepted(c.getAccepted());
                    return rc;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getAcceptanceRates() {
        List<Object[]> results = commentRepository.getAcceptanceRates();
        Map<String, Double> metrics = new HashMap<>();
        for (Object[] row : results) {
            String agentType = (String) row[0];
            Double acceptanceRate = ((Number) row[1]).doubleValue();
            metrics.put(agentType, acceptanceRate);
        }
        return metrics;
    }
}
