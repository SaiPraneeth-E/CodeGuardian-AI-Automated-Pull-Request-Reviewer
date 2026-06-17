package com.codeguardian.domain.model;

import java.util.List;

public class ReviewReport {
    private final String summary;
    private final Integer overallScore;
    private final List<ReviewComment> comments;

    public ReviewReport(String summary, Integer overallScore, List<ReviewComment> comments) {
        this.summary = summary;
        this.overallScore = overallScore;
        this.comments = comments;
    }

    public String getSummary() { return summary; }
    public Integer getOverallScore() { return overallScore; }
    public List<ReviewComment> getComments() { return comments; }

    @Override
    public String toString() {
        return "ReviewReport{" +
                "summary='" + summary + '\'' +
                ", overallScore=" + overallScore +
                ", commentsSize=" + (comments != null ? comments.size() : 0) +
                '}';
    }
}
