package com.codeguardian.domain.model;

import java.util.UUID;

/**
 * Domain model representing a Pull Request code review task processed asynchronously via Kafka.
 */
public class ReviewJob {
    private UUID pullRequestId;
    private String owner;
    private String repositoryName;
    private int prNumber;
    private String headSha;
    private String baseSha;

    public ReviewJob() {}

    public ReviewJob(UUID pullRequestId, String owner, String repositoryName, int prNumber, String headSha, String baseSha) {
        this.pullRequestId = pullRequestId;
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.prNumber = prNumber;
        this.headSha = headSha;
        this.baseSha = baseSha;
    }

    public UUID getPullRequestId() { return pullRequestId; }
    public void setPullRequestId(UUID pullRequestId) { this.pullRequestId = pullRequestId; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }

    public int getPrNumber() { return prNumber; }
    public void setPrNumber(int prNumber) { this.prNumber = prNumber; }

    public String getHeadSha() { return headSha; }
    public void setHeadSha(String headSha) { this.headSha = headSha; }

    public String getBaseSha() { return baseSha; }
    public void setBaseSha(String baseSha) { this.baseSha = baseSha; }

    @Override
    public String toString() {
        return "ReviewJob{" +
                "pullRequestId=" + pullRequestId +
                ", owner='" + owner + '\'' +
                ", repositoryName='" + repositoryName + '\'' +
                ", prNumber=" + prNumber +
                ", headSha='" + headSha + '\'' +
                ", baseSha='" + baseSha + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID pullRequestId;
        private String owner;
        private String repositoryName;
        private int prNumber;
        private String headSha;
        private String baseSha;

        public Builder pullRequestId(UUID pullRequestId) { this.pullRequestId = pullRequestId; return this; }
        public Builder owner(String owner) { this.owner = owner; return this; }
        public Builder repositoryName(String repositoryName) { this.repositoryName = repositoryName; return this; }
        public Builder prNumber(int prNumber) { this.prNumber = prNumber; return this; }
        public Builder headSha(String headSha) { this.headSha = headSha; return this; }
        public Builder baseSha(String baseSha) { this.baseSha = baseSha; return this; }

        public ReviewJob build() {
            return new ReviewJob(pullRequestId, owner, repositoryName, prNumber, headSha, baseSha);
        }
    }
}
