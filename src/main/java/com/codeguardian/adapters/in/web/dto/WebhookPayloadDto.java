package com.codeguardian.adapters.in.web.dto;

import com.codeguardian.domain.model.PullRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayloadDto {

    @NotBlank(message = "Action cannot be blank")
    private String action;

    private Integer number;

    @JsonProperty("pull_request")
    @NotNull(message = "pull_request details are required")
    @Valid
    private PullRequestDto pullRequest;

    @NotNull(message = "repository details are required")
    @Valid
    private RepositoryDto repository;

    public WebhookPayloadDto() {}

    public WebhookPayloadDto(String action, Integer number, PullRequestDto pullRequest, RepositoryDto repository) {
        this.action = action;
        this.number = number;
        this.pullRequest = pullRequest;
        this.repository = repository;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public PullRequestDto getPullRequest() { return pullRequest; }
    public void setPullRequest(PullRequestDto pullRequest) { this.pullRequest = pullRequest; }

    public RepositoryDto getRepository() { return repository; }
    public void setRepository(RepositoryDto repository) { this.repository = repository; }

    public PullRequest toDomain() {
        if (pullRequest == null || repository == null || repository.getOwner() == null) {
            throw new IllegalArgumentException("Incomplete webhook payload data for Domain translation.");
        }
        return PullRequest.builder()
                .githubPrId(pullRequest.getId())
                .prNumber(pullRequest.getNumber() != null ? pullRequest.getNumber() : number)
                .repositoryName(repository.getName())
                .owner(repository.getOwner().getLogin())
                .title(pullRequest.getTitle())
                .state(pullRequest.getState())
                .headSha(pullRequest.getHead() != null ? pullRequest.getHead().getSha() : "")
                .baseSha(pullRequest.getBase() != null ? pullRequest.getBase().getSha() : "")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .author(pullRequest.getUser() != null ? pullRequest.getUser().getLogin() : "")
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequestDto {
        @NotNull(message = "Pull Request ID is required")
        private Long id;

        @NotNull(message = "Pull Request number is required")
        private Integer number;

        @NotBlank(message = "Pull Request title is required")
        private String title;

        @NotBlank(message = "Pull Request state is required")
        private String state;

        @NotNull(message = "Pull Request head commit is required")
        @Valid
        private HeadDto head;

        @NotNull(message = "Pull Request base commit is required")
        @Valid
        private BaseDto base;

        private UserDto user;

        public PullRequestDto() {}

        public UserDto getUser() { return user; }
        public void setUser(UserDto user) { this.user = user; }

        public PullRequestDto(Long id, Integer number, String title, String state, HeadDto head, BaseDto base) {
            this.id = id;
            this.number = number;
            this.title = title;
            this.state = state;
            this.head = head;
            this.base = base;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Integer getNumber() { return number; }
        public void setNumber(Integer number) { this.number = number; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public HeadDto getHead() { return head; }
        public void setHead(HeadDto head) { this.head = head; }

        public BaseDto getBase() { return base; }
        public void setBase(BaseDto base) { this.base = base; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeadDto {
        @NotBlank(message = "Head SHA is required")
        private String sha;

        public HeadDto() {}
        public HeadDto(String sha) { this.sha = sha; }

        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaseDto {
        @NotBlank(message = "Base SHA is required")
        private String sha;

        public BaseDto() {}
        public BaseDto(String sha) { this.sha = sha; }

        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryDto {
        @NotBlank(message = "Repository name is required")
        private String name;

        @NotNull(message = "Repository owner is required")
        @Valid
        private OwnerDto owner;

        public RepositoryDto() {}
        public RepositoryDto(String name, OwnerDto owner) {
            this.name = name;
            this.owner = owner;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public OwnerDto getOwner() { return owner; }
        public void setOwner(OwnerDto owner) { this.owner = owner; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OwnerDto {
        @NotBlank(message = "Owner login username is required")
        private String login;

        public OwnerDto() {}
        public OwnerDto(String login) { this.login = login; }

        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserDto {
        private String login;

        public UserDto() {}
        public UserDto(String login) { this.login = login; }

        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
    }
}
