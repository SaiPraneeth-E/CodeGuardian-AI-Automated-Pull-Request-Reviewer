package com.codeguardian.usecases.service;

import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.domain.model.Review;
import com.codeguardian.domain.model.ReviewReport;
import com.codeguardian.domain.model.ReviewStatus;
import com.codeguardian.usecases.port.in.GenerateReviewUseCase;
import com.codeguardian.usecases.port.out.GitHubClientPort;
import com.codeguardian.usecases.port.out.ReviewRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.codeguardian.domain.model.CodeSnippet;
import com.codeguardian.usecases.port.out.EmbeddingClientPort;
import com.codeguardian.usecases.port.out.VectorStorePort;

/**
 * Orchestrator service linking pull request events, AI analysis, and GitHub API responses.
 */
@Service
public class ReviewOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ReviewOrchestrationService.class);

    private final GenerateReviewUseCase generateReviewUseCase;
    private final GitHubClientPort githubClient;
    private final ReviewRepositoryPort reviewRepository;
    private final EmbeddingClientPort embeddingClient;
    private final VectorStorePort vectorStore;

    public ReviewOrchestrationService(GenerateReviewUseCase generateReviewUseCase,
                                      GitHubClientPort githubClient,
                                      ReviewRepositoryPort reviewRepository,
                                      EmbeddingClientPort embeddingClient,
                                      VectorStorePort vectorStore) {
        this.generateReviewUseCase = generateReviewUseCase;
        this.githubClient = githubClient;
        this.reviewRepository = reviewRepository;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public String fetchPullRequestDiff(String owner, String repositoryName, int prNumber) {
        return githubClient.fetchPullRequestDiff(owner, repositoryName, prNumber);
    }

    /**
     * Runs the orchestrated review pipeline: generates reviews via LLM, publishes reports to GitHub, and logs status.
     *
     * @param pullRequest details of the pull request being reviewed
     * @param diffContent git diff text content to review
     */
    public void orchestrateReview(PullRequest pullRequest, String diffContent) {
        log.info("Triggering orchestrated AI review pipeline for: {}/{} PR #{}",
                pullRequest.getOwner(), pullRequest.getRepositoryName(), pullRequest.getPrNumber());

        if (diffContent == null || diffContent.trim().isEmpty()) {
            log.warn("Bypassing review run: Diff contents are blank.");
            return;
        }

        // Optimize the diff content by stripping deleted files to avoid token/rate limits
        String optimizedDiff = DiffPreprocessor.preprocessDiff(diffContent);

        try {
            // Retrieve semantic context from codebase index (RAG)
            String codebaseContext = null;
            try {
                log.info("Retrieving codebase context matching PR diff...");
                float[] diffEmbedding = embeddingClient.generateEmbedding(optimizedDiff);
                List<CodeSnippet> snippets = vectorStore.findSimilar(diffEmbedding, 3);
                if (!snippets.isEmpty()) {
                    StringBuilder contextBuilder = new StringBuilder();
                    for (CodeSnippet snippet : snippets) {
                        contextBuilder.append("File: ").append(snippet.getFilePath())
                                .append(" (Lines ").append(snippet.getStartLine()).append("-").append(snippet.getEndLine()).append(")\n")
                                .append("Code:\n")
                                .append(snippet.getContent()).append("\n\n");
                    }
                    codebaseContext = contextBuilder.toString().trim();
                    log.info("Retrieved {} relevant codebase snippets from vector index.", snippets.size());
                } else {
                    log.info("No relevant codebase snippets found in vector index.");
                }
            } catch (Exception ex) {
                log.warn("Codebase context retrieval failed (bypassing RAG): {}", ex.getMessage());
            }

            // 1. Generate code review report utilizing Gemini API with RAG context
            ReviewReport report = generateReviewUseCase.execute(optimizedDiff, codebaseContext, pullRequest.getRepositoryName());

            // 2. Format overall review report markdown body
            String overallSummary = String.format(
                    "### 🛡️ CodeGuardian AI Pull Request Review\n\n" +
                    "**Overall Code Quality Score:** `%d/100`\n\n" +
                    "#### Summary Recommendation:\n%s",
                    report.getOverallScore(),
                    report.getSummary()
            );

            // 3. Post review report back to GitHub Pull Request
            githubClient.postReview(
                    pullRequest.getOwner(),
                    pullRequest.getRepositoryName(),
                    pullRequest.getPrNumber(),
                    overallSummary,
                    report.getComments(),
                    report.getOverallScore(),
                    pullRequest.getAuthor()
            );

            // 4. Persist review history to PostgreSQL
            if (pullRequest.getId() == null) {
                log.warn("Bypassing review persistence: PullRequest has no database ID.");
            } else {
                Review review = Review.builder()
                        .id(UUID.randomUUID())
                        .pullRequestId(pullRequest.getId())
                        .status(ReviewStatus.COMPLETED)
                        .summary(report.getSummary())
                        .overallScore(report.getOverallScore())
                        .createdAt(Instant.now())
                        .completedAt(Instant.now())
                        .comments(report.getComments())
                        .build();
                reviewRepository.save(review);
                log.info("Successfully persisted completed review log in PostgreSQL.");
            }

            log.info("Orchestrated review pipeline completed successfully for PR #{}", pullRequest.getPrNumber());

        } catch (Exception ex) {
            log.error("Failed executing orchestrated review pipeline for PR #{}: {}", pullRequest.getPrNumber(), ex.getMessage());

            // Persist failed review log to PostgreSQL
            if (pullRequest.getId() != null) {
                try {
                    Review failedReview = Review.builder()
                            .id(UUID.randomUUID())
                            .pullRequestId(pullRequest.getId())
                            .status(ReviewStatus.FAILED)
                            .summary("Review failed: " + ex.getMessage())
                            .overallScore(0)
                            .createdAt(Instant.now())
                            .completedAt(Instant.now())
                            .comments(Collections.emptyList())
                            .build();
                    reviewRepository.save(failedReview);
                    log.info("Persisted failed review log in PostgreSQL.");
                } catch (Exception dbEx) {
                    log.error("Failed to persist failed review status to database: {}", dbEx.getMessage());
                }
            }

            // Re-throw exception to propagate failures back to consumer/retry policy
            throw ex;
        }
    }
}
