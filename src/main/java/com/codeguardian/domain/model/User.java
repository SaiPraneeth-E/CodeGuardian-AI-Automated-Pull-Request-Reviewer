package com.codeguardian.domain.model;

import java.time.Instant;
import java.util.UUID;

public class User {
    private final UUID id;
    private final Long githubUserId;
    private final String login;
    private final String email;
    private final String avatarUrl;
    private final String oauthTokenEncrypted;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User(UUID id, Long githubUserId, String login, String email, String avatarUrl, 
                String oauthTokenEncrypted, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.githubUserId = githubUserId;
        this.login = login;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.oauthTokenEncrypted = oauthTokenEncrypted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public Long getGithubUserId() { return githubUserId; }
    public String getLogin() { return login; }
    public String getEmail() { return email; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getOauthTokenEncrypted() { return oauthTokenEncrypted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .githubUserId(this.githubUserId)
                .login(this.login)
                .email(this.email)
                .avatarUrl(this.avatarUrl)
                .oauthTokenEncrypted(this.oauthTokenEncrypted)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Long githubUserId;
        private String login;
        private String email;
        private String avatarUrl;
        private String oauthTokenEncrypted;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder githubUserId(Long githubUserId) { this.githubUserId = githubUserId; return this; }
        public Builder login(String login) { this.login = login; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder oauthTokenEncrypted(String oauthTokenEncrypted) { this.oauthTokenEncrypted = oauthTokenEncrypted; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public User build() {
            return new User(id, githubUserId, login, email, avatarUrl, oauthTokenEncrypted, createdAt, updatedAt);
        }
    }
}
