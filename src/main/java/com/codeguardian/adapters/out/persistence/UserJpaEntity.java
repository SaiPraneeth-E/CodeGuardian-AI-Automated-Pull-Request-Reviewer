package com.codeguardian.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "github_user_id", unique = true, nullable = false)
    private Long githubUserId;

    @Column(name = "login", unique = true, nullable = false)
    private String login;

    @Column(name = "email")
    private String email;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "oauth_token_encrypted")
    private String oauthTokenEncrypted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserJpaEntity() {}

    public UserJpaEntity(UUID id, Long githubUserId, String login, String email, String avatarUrl, 
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
    public void setId(UUID id) { this.id = id; }

    public Long getGithubUserId() { return githubUserId; }
    public void setGithubUserId(Long githubUserId) { this.githubUserId = githubUserId; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getOauthTokenEncrypted() { return oauthTokenEncrypted; }
    public void setOauthTokenEncrypted(String oauthTokenEncrypted) { this.oauthTokenEncrypted = oauthTokenEncrypted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

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

        public UserJpaEntity build() {
            return new UserJpaEntity(id, githubUserId, login, email, avatarUrl, oauthTokenEncrypted, createdAt, updatedAt);
        }
    }
}
