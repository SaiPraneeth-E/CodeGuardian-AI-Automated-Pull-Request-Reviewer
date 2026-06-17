package com.codeguardian.domain.model;

import java.time.Instant;
import java.util.UUID;

public class UserConnectedRepository {
    private final UUID id;
    private final UUID userId;
    private final Long githubRepoId;
    private final String name;
    private final String owner;
    private final Long webhookId;
    private final String webhookSecret;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public UserConnectedRepository(UUID id, UUID userId, Long githubRepoId, String name, String owner, 
                                   Long webhookId, String webhookSecret, boolean active, 
                                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.githubRepoId = githubRepoId;
        this.name = name;
        this.owner = owner;
        this.webhookId = webhookId;
        this.webhookSecret = webhookSecret;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Long getGithubRepoId() { return githubRepoId; }
    public String getName() { return name; }
    public String getOwner() { return owner; }
    public Long getWebhookId() { return webhookId; }
    public String getWebhookSecret() { return webhookSecret; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private Long githubRepoId;
        private String name;
        private String owner;
        private Long webhookId;
        private String webhookSecret;
        private boolean active = true;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder githubRepoId(Long githubRepoId) { this.githubRepoId = githubRepoId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder owner(String owner) { this.owner = owner; return this; }
        public Builder webhookId(Long webhookId) { this.webhookId = webhookId; return this; }
        public Builder webhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserConnectedRepository build() {
            return new UserConnectedRepository(id, userId, githubRepoId, name, owner, webhookId, webhookSecret, active, createdAt, updatedAt);
        }
    }
}
