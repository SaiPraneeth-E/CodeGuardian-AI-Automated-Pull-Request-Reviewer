package com.codeguardian.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pull_requests")
public class PullRequestJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "github_pr_id", unique = true, nullable = false)
    private Long githubPrId;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "repository_name", nullable = false)
    private String repositoryName;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "state", length = 50, nullable = false)
    private String state;

    @Column(name = "head_sha", length = 100, nullable = false)
    private String headSha;

    @Column(name = "base_sha", length = 100, nullable = false)
    private String baseSha;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PullRequestJpaEntity() {}

    public PullRequestJpaEntity(UUID id, Long githubPrId, Integer prNumber, String repositoryName, String owner,
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
    public void setId(UUID id) { this.id = id; }

    public Long getGithubPrId() { return githubPrId; }
    public void setGithubPrId(Long githubPrId) { this.githubPrId = githubPrId; }

    public Integer getPrNumber() { return prNumber; }
    public void setPrNumber(Integer prNumber) { this.prNumber = prNumber; }

    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getHeadSha() { return headSha; }
    public void setHeadSha(String headSha) { this.headSha = headSha; }

    public String getBaseSha() { return baseSha; }
    public void setBaseSha(String baseSha) { this.baseSha = baseSha; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

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

        public PullRequestJpaEntity build() {
            return new PullRequestJpaEntity(id, githubPrId, prNumber, repositoryName, owner, title, state, headSha, baseSha, createdAt, updatedAt);
        }
    }
}
