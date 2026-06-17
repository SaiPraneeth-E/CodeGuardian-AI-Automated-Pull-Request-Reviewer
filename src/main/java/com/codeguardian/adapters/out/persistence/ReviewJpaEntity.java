package com.codeguardian.adapters.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class ReviewJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequestJpaEntity pullRequest;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewCommentJpaEntity> comments = new ArrayList<>();

    public ReviewJpaEntity() {}

    public ReviewJpaEntity(UUID id, PullRequestJpaEntity pullRequest, String status, String summary,
                           Integer overallScore, Instant createdAt, Instant completedAt, List<ReviewCommentJpaEntity> comments) {
        this.id = id;
        this.pullRequest = pullRequest;
        this.status = status;
        this.summary = summary;
        this.overallScore = overallScore;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        if (comments != null) {
            this.comments = comments;
            for (ReviewCommentJpaEntity comment : comments) {
                comment.setReview(this);
            }
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PullRequestJpaEntity getPullRequest() { return pullRequest; }
    public void setPullRequest(PullRequestJpaEntity pullRequest) { this.pullRequest = pullRequest; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public List<ReviewCommentJpaEntity> getComments() { return comments; }
    public void setComments(List<ReviewCommentJpaEntity> comments) {
        this.comments = comments;
        if (comments != null) {
            for (ReviewCommentJpaEntity comment : comments) {
                comment.setReview(this);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private PullRequestJpaEntity pullRequest;
        private String status;
        private String summary;
        private Integer overallScore;
        private Instant createdAt;
        private Instant completedAt;
        private List<ReviewCommentJpaEntity> comments = new ArrayList<>();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder pullRequest(PullRequestJpaEntity pullRequest) { this.pullRequest = pullRequest; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder overallScore(Integer overallScore) { this.overallScore = overallScore; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder completedAt(Instant completedAt) { this.completedAt = completedAt; return this; }
        public Builder comments(List<ReviewCommentJpaEntity> comments) { this.comments = comments; return this; }

        public ReviewJpaEntity build() {
            return new ReviewJpaEntity(id, pullRequest, status, summary, overallScore, createdAt, completedAt, comments);
        }
    }
}
