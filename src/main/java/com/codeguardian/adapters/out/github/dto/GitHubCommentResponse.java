package com.codeguardian.adapters.out.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO to deserialize comments returned by the GitHub API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubCommentResponse {
    private String path;
    private Integer line;
    private String body;

    public GitHubCommentResponse() {}

    public GitHubCommentResponse(String path, Integer line, String body) {
        this.path = path;
        this.line = line;
        this.body = body;
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Integer getLine() { return line; }
    public void setLine(Integer line) { this.line = line; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
