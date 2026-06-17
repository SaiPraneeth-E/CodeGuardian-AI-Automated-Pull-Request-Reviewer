package com.codeguardian.adapters.out.github.dto;

import java.util.List;

/**
 * DTO to serialize the Pull Request Review creation payload to the GitHub API.
 */
public class GitHubReviewRequest {
    private String body;
    private String event; // e.g. "COMMENT", "APPROVE", "REQUEST_CHANGES"
    private List<DraftComment> comments;

    public GitHubReviewRequest() {}

    public GitHubReviewRequest(String body, String event, List<DraftComment> comments) {
        this.body = body;
        this.event = event;
        this.comments = comments;
    }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public List<DraftComment> getComments() { return comments; }
    public void setComments(List<DraftComment> comments) { this.comments = comments; }

    public static class DraftComment {
        private String path;
        private Integer line;
        private String body;

        public DraftComment() {}

        public DraftComment(String path, Integer line, String body) {
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
}
