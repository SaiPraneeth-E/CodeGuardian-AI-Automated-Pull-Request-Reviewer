package com.codeguardian.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain entity representing a pull request review.
 */
public class Review {
    private final UUID id;
    private final UUID pullRequestId;
    private final ReviewStatus status;
    private final String summary;
    private final Integer overallScore;
    private final Instant createdAt;
    private final Instant completedAt;
    private final List<ReviewComment> comments;

    public Review(UUID id, UUID pullRequestId, ReviewStatus status, String summary,
                  Integer overallScore, Instant createdAt, Instant completedAt, List<ReviewComment> comments) {
        this.id = id;
        this.pullRequestId = pullRequestId;
        this.status = status;
        this.summary = summary;
        this.overallScore = overallScore;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.comments = comments;
    }

    public UUID getId() { return id; }
    public UUID getPullRequestId() { return pullRequestId; }
    public ReviewStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public Integer getOverallScore() { return overallScore; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<ReviewComment> getComments() { return comments; }

    @Override
    public String toString() {
        return "Review{" +
                "id=" + id +
                ", pullRequestId=" + pullRequestId +
                ", status=" + status +
                ", overallScore=" + overallScore +
                ", commentsSize=" + (comments != null ? comments.size() : 0) +
                '}';
    }

    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .pullRequestId(this.pullRequestId)
                .status(this.status)
                .summary(this.summary)
                .overallScore(this.overallScore)
                .createdAt(this.createdAt)
                .completedAt(this.completedAt)
                .comments(this.comments);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID pullRequestId;
        private ReviewStatus status;
        private String summary;
        private Integer overallScore;
        private Instant createdAt;
        private Instant completedAt;
        private List<ReviewComment> comments;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder pullRequestId(UUID pullRequestId) { this.pullRequestId = pullRequestId; return this; }
        public Builder status(ReviewStatus status) { this.status = status; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder overallScore(Integer overallScore) { this.overallScore = overallScore; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder completedAt(Instant completedAt) { this.completedAt = completedAt; return this; }
        public Builder comments(List<ReviewComment> comments) { this.comments = comments; return this; }

        public Review build() {
            return new Review(id, pullRequestId, status, summary, overallScore, createdAt, completedAt, comments);
        }
    }
}
