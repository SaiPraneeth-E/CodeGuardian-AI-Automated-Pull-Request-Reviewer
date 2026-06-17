package com.codeguardian.usecases.port.out;

import com.codeguardian.domain.model.ReviewComment;

import java.util.List;

/**
 * Output port for executing Pull Request reviews and comment retrieval via GitHub APIs.
 */
public interface GitHubClientPort {
    /**
     * Submits an atomic Pull Request review (overall summary and inline file comments).
     *
     * @param owner          repository owner
     * @param repositoryName repository name
     * @param prNumber       pull request ID number
     * @param summary        overall markdown summary
     * @param comments       list of inline code comments
     * @param overallScore   aggregated code quality score
     * @param author         PR author handle for notification mentions
     */
    void postReview(String owner, String repositoryName, int prNumber, String summary, List<ReviewComment> comments, int overallScore, String author);

    /**
     * Retrieves all existing review comments previously posted on the pull request.
     *
     * @param owner          repository owner
     * @param repositoryName repository name
     * @param prNumber       pull request ID number
     * @return list of existing comments
     */
    List<ReviewComment> fetchExistingComments(String owner, String repositoryName, int prNumber);

    /**
     * Fetches the raw pull request diff from GitHub.
     *
     * @param owner          repository owner
     * @param repositoryName repository name
     * @param prNumber       pull request ID number
     * @return raw diff text content
     */
    String fetchPullRequestDiff(String owner, String repositoryName, int prNumber);
}
