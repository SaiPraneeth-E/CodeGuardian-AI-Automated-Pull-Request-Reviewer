package com.codeguardian.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PullRequest {
    private final UUID id;
    private final Long githubPrId;
    private final Integer prNumber;
    private final String repositoryName;
    private final String owner;
    private final String title;
    private final String state;
    private final String headSha;
    private final String baseSha;
    private final Instant createdAt;
    private final Instant updatedAt;
    private String author = "";

    public PullRequest(UUID id, Long githubPrId, Integer prNumber, String repositoryName, String owner,
                       String title, String state, String headSha, String baseSha, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.githubPrId = githubPrId;
        this.prNumber = prNumber;
        this.repositoryName = repositoryName;
        this.owner = owner;
        this.title = title;
        this.state = state;
        this.headSha = headSha;
        this.baseSha = baseSha;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public Long getGithubPrId() { return githubPrId; }
    public Integer getPrNumber() { return prNumber; }
    public String getRepositoryName() { return repositoryName; }
    public String getOwner() { return owner; }
    public String getTitle() { return title; }
    public String getState() { return state; }
    public String getHeadSha() { return headSha; }
    public String getBaseSha() { return baseSha; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public boolean isOpen() {
        return "open".equalsIgnoreCase(state);
    }

    public boolean isValid() {
        return githubPrId != null && prNumber != null && repositoryName != null && owner != null && state != null;
    }

    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .githubPrId(this.githubPrId)
                .prNumber(this.prNumber)
                .repositoryName(this.repositoryName)
                .owner(this.owner)
                .title(this.title)
                .state(this.state)
                .headSha(this.headSha)
                .baseSha(this.baseSha)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .author(this.author);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Long githubPrId;
        private Integer prNumber;
        private String repositoryName;
        private String owner;
        private String title;
        private String state;
        private String headSha;
        private String baseSha;
        private Instant createdAt;
        private Instant updatedAt;
        private String author = "";

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder githubPrId(Long githubPrId) { this.githubPrId = githubPrId; return this; }
        public Builder prNumber(Integer prNumber) { this.prNumber = prNumber; return this; }
        public Builder repositoryName(String repositoryName) { this.repositoryName = repositoryName; return this; }
        public Builder owner(String owner) { this.owner = owner; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder state(String state) { this.state = state; return this; }
        public Builder headSha(String headSha) { this.headSha = headSha; return this; }
        public Builder baseSha(String baseSha) { this.baseSha = baseSha; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder author(String author) { this.author = author; return this; }

        public PullRequest build() {
            PullRequest pr = new PullRequest(id, githubPrId, prNumber, repositoryName, owner, title, state, headSha, baseSha, createdAt, updatedAt);
            pr.setAuthor(author);
            return pr;
        }
    }
}
