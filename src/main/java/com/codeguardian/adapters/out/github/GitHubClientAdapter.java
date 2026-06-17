package com.codeguardian.adapters.out.github;

import com.codeguardian.adapters.out.github.dto.GitHubCommentResponse;
import com.codeguardian.adapters.out.github.dto.GitHubReviewRequest;
import com.codeguardian.domain.exception.GitHubApiException;
import com.codeguardian.domain.model.ReviewComment;
import com.codeguardian.usecases.port.out.GitHubClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter integrating RestClient to communicate directly with GitHub REST APIs.
 */
@Component
public class GitHubClientAdapter implements GitHubClientPort {

    private static final Logger log = LoggerFactory.getLogger(GitHubClientAdapter.class);
    private static final String SIGNATURE_TAG = "<!-- System: CodeGuardianAI -->";

    private final RestClient restClient;

    @Value("${codeguardian.github.token}")
    private String token;

    @Value("${codeguardian.github.api-url:https://api.github.com}")
    private String apiUrl;

    @Value("${codeguardian.github.approval-threshold:80}")
    private int approvalThreshold;

    public GitHubClientAdapter(RestClient geminiRestClient) {
        this.restClient = geminiRestClient;
    }

    @Override
    public List<ReviewComment> fetchExistingComments(String owner, String repositoryName, int prNumber) {
        log.info("Fetching existing PR comments for repository: {}/{} PR #{}", owner, repositoryName, prNumber);

        try {
            ResponseEntity<List<GitHubCommentResponse>> responseEntity = restClient.get()
                    .uri(apiUrl + "/repos/" + owner + "/" + repositoryName + "/pulls/" + prNumber + "/comments")
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<GitHubCommentResponse>>() {});

            List<GitHubCommentResponse> responseList = responseEntity.getBody();
            if (responseList == null) {
                return List.of();
            }

            return responseList.stream()
                    .map(dto -> new ReviewComment(dto.getPath(), dto.getLine(), "IMPORTED", dto.getBody()))
                    .collect(Collectors.toList());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("GitHub API communication failure during comment fetch: {} {}", e.getStatusCode(), e.getStatusText());
            throw new GitHubApiException("GitHub API returned error code " + e.getStatusCode(), e.getStatusCode().value(), e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving GitHub comments: {}", e.getMessage());
            throw new GitHubApiException("Unexpected failure fetching comments", HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    @Override
    public void postReview(String owner, String repositoryName, int prNumber, String summary, List<ReviewComment> comments, int overallScore, String author) {
        log.info("Posting review report back to repository: {}/{} PR #{} (Score: {}, Threshold: {})", owner, repositoryName, prNumber, overallScore, approvalThreshold);

        if (token == null || token.trim().isEmpty() || "mock-github-token-for-testing".equals(token)) {
            log.warn("Skipping GitHub API submission: Token is mock or blank.");
            return;
        }

        // 1. Fetch existing comments to prevent double-posting
        List<ReviewComment> existingComments = fetchExistingComments(owner, repositoryName, prNumber);
        log.debug("Found {} existing comments on the PR.", existingComments.size());

        List<GitHubReviewRequest.DraftComment> newCommentsToPost = new ArrayList<>();

        // 2. Filter new review comments for duplicates
        for (ReviewComment comment : comments) {
            if (comment.getFilePath() == null || comment.getLineNumber() == null) {
                // Skip overall summary comments from inline files
                continue;
            }

            String annotatedBody = comment.getCommentText() + "\n\n" + SIGNATURE_TAG;

            boolean exists = existingComments.stream().anyMatch(existing -> 
                    existing.getFilePath() != null && existing.getFilePath().equalsIgnoreCase(comment.getFilePath()) &&
                    existing.getLineNumber() != null && existing.getLineNumber().equals(comment.getLineNumber()) &&
                    existing.getCommentText() != null && existing.getCommentText().contains(comment.getCommentText())
            );

            if (!exists) {
                newCommentsToPost.add(new GitHubReviewRequest.DraftComment(
                        comment.getFilePath(),
                        comment.getLineNumber(),
                        annotatedBody
                ));
            } else {
                log.info("Filter duplicate comment: File '{}' Line '{}' already reviewed.",
                        comment.getFilePath(), comment.getLineNumber());
            }
        }

        // 3. Assemble atomic review request
        String eventType = overallScore >= approvalThreshold ? "APPROVE" : "REQUEST_CHANGES";
        String mentionText = (author != null && !author.trim().isEmpty()) ? "Cc: @" + author.trim() + "\n\n" : "";

        String finalReviewSummary = mentionText + summary + "\n\n" + SIGNATURE_TAG;
        GitHubReviewRequest reviewPayload = new GitHubReviewRequest(
                finalReviewSummary,
                eventType,
                newCommentsToPost
        );

        // 4. POST Review to GitHub pulls reviews endpoint
        try {
            restClient.post()
                    .uri(apiUrl + "/repos/" + owner + "/" + repositoryName + "/pulls/" + prNumber + "/reviews")
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reviewPayload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully submitted code review for PR #{} with {} inline comments.",
                    prNumber, newCommentsToPost.size());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 422) {
                String responseBody = e.getResponseBodyAsString();
                log.warn("GitHub review posting returned 422. Retrying with comments appended to the summary body. Details: {}", responseBody);

                // Construct fallback summary body with inline comments appended as markdown
                StringBuilder fallbackSummary = new StringBuilder(mentionText + summary);
                if (!newCommentsToPost.isEmpty()) {
                    fallbackSummary.append("\n\n---\n\n### 📝 Detailed Inline Suggestions\n\n");
                    for (GitHubReviewRequest.DraftComment c : newCommentsToPost) {
                        String bodyText = c.getBody().replace(SIGNATURE_TAG, "").trim();
                        fallbackSummary.append("- **File:** `").append(c.getPath()).append("` (Line ").append(c.getLine()).append(")\n")
                                       .append("  ").append(bodyText).append("\n\n");
                    }
                }
                fallbackSummary.append("\n\n").append(SIGNATURE_TAG);

                // Fall back to COMMENT if self-review is detected
                String fallbackEvent = eventType;
                if (responseBody.contains("own pull request") || responseBody.contains("own Pull Request")) {
                    fallbackEvent = "COMMENT";
                }

                GitHubReviewRequest fallbackPayload = new GitHubReviewRequest(
                        fallbackSummary.toString(),
                        fallbackEvent,
                        List.of() // Empty list of inline comments to avoid 422 validation errors
                );

                try {
                    restClient.post()
                            .uri(apiUrl + "/repos/" + owner + "/" + repositoryName + "/pulls/" + prNumber + "/reviews")
                            .header("Authorization", "Bearer " + token)
                            .header("Accept", "application/vnd.github+json")
                            .header("X-GitHub-Api-Version", "2022-11-28")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(fallbackPayload)
                            .retrieve()
                            .toBodilessEntity();
                    log.info("Successfully submitted fallback code review for PR #{} with comments in summary body.", prNumber);
                } catch (Exception ex) {
                    log.error("Failed executing fallback review submission for PR #{}: {}", prNumber, ex.getMessage());
                    throw new GitHubApiException("GitHub fallback review post error: " + ex.getMessage(), e.getStatusCode().value(), ex);
                }
            } else {
                log.error("GitHub API review posting failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new GitHubApiException("GitHub review post error: " + e.getResponseBodyAsString(), e.getStatusCode().value(), e);
            }
        } catch (HttpServerErrorException e) {
            log.error("GitHub API review posting failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GitHubApiException("GitHub review post error: " + e.getResponseBodyAsString(), e.getStatusCode().value(), e);
        } catch (Exception e) {
            log.error("Unexpected error submitting review to GitHub: {}", e.getMessage());
            throw new GitHubApiException("Unexpected failure posting code review", HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    @Override
    public String fetchPullRequestDiff(String owner, String repositoryName, int prNumber) {
        log.info("Fetching diff for repository: {}/{} PR #{}", owner, repositoryName, prNumber);
        try {
            RestClient.RequestHeadersSpec<?> requestSpec = restClient.get()
                    .uri(apiUrl + "/repos/" + owner + "/" + repositoryName + "/pulls/" + prNumber)
                    .header("Accept", "application/vnd.github.v3.diff")
                    .header("X-GitHub-Api-Version", "2022-11-28");

            if (token != null && !token.trim().isEmpty() && !"mock-github-token-for-testing".equals(token)) {
                requestSpec = requestSpec.header("Authorization", "Bearer " + token);
            }

            ResponseEntity<String> responseEntity = requestSpec
                    .retrieve()
                    .toEntity(String.class);

            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("GitHub API diff fetch failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GitHubApiException("GitHub diff fetch error: " + e.getResponseBodyAsString(), e.getStatusCode().value(), e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving GitHub diff: {}", e.getMessage());
            throw new GitHubApiException("Unexpected failure fetching diff", HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }
}
