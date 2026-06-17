package com.codeguardian.domain.model;

public class ReviewComment {
    private java.util.UUID id;
    private String filePath;
    private Integer lineNumber;
    private String category; // e.g. BUG, PERFORMANCE, SECURITY, TESTING, STYLE
    private String commentText;
    private Boolean accepted;

    public ReviewComment() {}

    public ReviewComment(String filePath, Integer lineNumber, String category, String commentText) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.category = category;
        this.commentText = commentText;
    }

    public java.util.UUID getId() { return id; }
    public void setId(java.util.UUID id) { this.id = id; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }

    @Override
    public String toString() {
        return "ReviewComment{" +
                "id=" + id +
                ", filePath='" + filePath + '\'' +
                ", lineNumber=" + lineNumber +
                ", category='" + category + '\'' +
                ", commentText='" + commentText + '\'' +
                ", accepted=" + accepted +
                '}';
    }
}
