package com.codeguardian.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_connected_repositories")
public class UserConnectedRepositoryJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "github_repo_id", nullable = false)
    private Long githubRepoId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "webhook_id")
    private Long webhookId;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserConnectedRepositoryJpaEntity() {}

    public UserConnectedRepositoryJpaEntity(UUID id, UUID userId, Long githubRepoId, String name, String owner, 
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
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getGithubRepoId() { return githubRepoId; }
    public void setGithubRepoId(Long githubRepoId) { this.githubRepoId = githubRepoId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public Long getWebhookId() { return webhookId; }
    public void setWebhookId(Long webhookId) { this.webhookId = webhookId; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

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
        private boolean active;
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

        public UserConnectedRepositoryJpaEntity build() {
            return new UserConnectedRepositoryJpaEntity(id, userId, githubRepoId, name, owner, webhookId, webhookSecret, active, createdAt, updatedAt);
        }
    }
}
