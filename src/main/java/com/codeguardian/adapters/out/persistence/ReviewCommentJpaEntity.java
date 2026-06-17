package com.codeguardian.adapters.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class ReviewCommentJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private ReviewJpaEntity review;

    @Column(name = "file_path", length = 512, nullable = false)
    private String filePath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "agent_type", length = 50, nullable = false)
    private String agentType; // Maps category of the comment, e.g., BUG, PERFORMANCE, etc.

    @Column(name = "comment_text", columnDefinition = "TEXT", nullable = false)
    private String commentText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted")
    private Boolean accepted;

    public ReviewCommentJpaEntity() {}

    public ReviewCommentJpaEntity(UUID id, ReviewJpaEntity review, String filePath, Integer lineNumber,
                                 String agentType, String commentText, Instant createdAt, Boolean accepted) {
        this.id = id;
        this.review = review;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.agentType = agentType;
        this.commentText = commentText;
        this.createdAt = createdAt;
        this.accepted = accepted;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ReviewJpaEntity getReview() { return review; }
    public void setReview(ReviewJpaEntity review) { this.review = review; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private ReviewJpaEntity review;
        private String filePath;
        private Integer lineNumber;
        private String agentType;
        private String commentText;
        private Instant createdAt;
        private Boolean accepted;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder review(ReviewJpaEntity review) { this.review = review; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder lineNumber(Integer lineNumber) { this.lineNumber = lineNumber; return this; }
        public Builder agentType(String agentType) { this.agentType = agentType; return this; }
        public Builder commentText(String commentText) { this.commentText = commentText; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder accepted(Boolean accepted) { this.accepted = accepted; return this; }

        public ReviewCommentJpaEntity build() {
            return new ReviewCommentJpaEntity(id, review, filePath, lineNumber, agentType, commentText, createdAt, accepted);
        }
    }
}
