package com.codeguardian.usecases.service;

import com.codeguardian.domain.model.ReviewAgentRole;
import com.codeguardian.domain.model.ReviewComment;
import com.codeguardian.domain.model.ReviewReport;
import com.codeguardian.usecases.port.in.GenerateReviewUseCase;
import com.codeguardian.usecases.port.out.GeminiClientPort;
import com.codeguardian.usecases.port.out.ReviewRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Usecase service coordinating code reviews using specialized AI agents running SEQUENTIALLY.
 * Sequential execution is required for Groq free-tier rate limits (limited RPM and TPM).
 * Uses 2 agents (SECURITY + ARCHITECTURE) to balance coverage vs. rate limit constraints.
 */
@Primary
@Service
public class MultiAgentReviewService implements GenerateReviewUseCase {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentReviewService.class);
    private static final long INTER_AGENT_DELAY_MS = 15000; // 15s between agent calls to stay under Groq rate limits

    private final GeminiClientPort geminiClient;
    private final ReviewRepositoryPort reviewRepositoryPort;

    public MultiAgentReviewService(GeminiClientPort geminiClient, ReviewRepositoryPort reviewRepositoryPort) {
        this.geminiClient = geminiClient;
        this.reviewRepositoryPort = reviewRepositoryPort;
    }

    @Override
    public ReviewReport execute(String diffContent) {
        return execute(diffContent, null);
    }

    @Override
    public ReviewReport execute(String diffContent, String codebaseContext) {
        return execute(diffContent, codebaseContext, null);
    }

    @Override
    public ReviewReport execute(String diffContent, String codebaseContext, String repositoryName) {
        log.info("Initiating Sequential Multi-Agent Code Review session.");

        if (diffContent == null || diffContent.trim().isEmpty()) {
            log.warn("Aborting code review: Empty diff content payload provided.");
            throw new IllegalArgumentException("Diff content payload cannot be blank.");
        }

        String feedbackContext = "";
        if (repositoryName != null && !repositoryName.trim().isEmpty()) {
            List<ReviewComment> recentFeedback = reviewRepositoryPort.getRecentFeedback(repositoryName, 5);
            if (recentFeedback != null && !recentFeedback.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ReviewComment c : recentFeedback) {
                    sb.append("- [").append(c.getAccepted() != null && c.getAccepted() ? "ACCEPTED" : "REJECTED").append("] ")
                      .append("Agent: ").append(c.getCategory()).append(" | ")
                      .append("Suggestion: ").append(c.getCommentText()).append("\n");
                }
                feedbackContext = sb.toString();
                log.info("Injected {} historical feedback items into AI prompt.", recentFeedback.size());
            }
        }

        // Use 2 agents to balance review coverage vs Groq free-tier rate limits.
        // SECURITY covers bugs + security; ARCHITECTURE covers design + performance + testing patterns.
        List<ReviewAgentRole> roles = List.of(
                ReviewAgentRole.SECURITY,
                ReviewAgentRole.ARCHITECTURE
        );

        // Execute agents SEQUENTIALLY with delays to avoid 429 rate limits
        List<ReviewReport> completedReports = new ArrayList<>();

        for (int i = 0; i < roles.size(); i++) {
            ReviewAgentRole role = roles.get(i);
            try {
                log.info("Dispatching review to {} agent ({}/{})...", role, i + 1, roles.size());
                ReviewReport report = geminiClient.generateReview(diffContent, codebaseContext, feedbackContext, role);
                completedReports.add(report);
                log.info("{} agent completed successfully.", role);

                // Wait between agent calls to respect Groq rate limits (skip after last agent)
                if (i < roles.size() - 1) {
                    log.info("Waiting {}ms between agent calls to respect rate limits...", INTER_AGENT_DELAY_MS);
                    try {
                        Thread.sleep(INTER_AGENT_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Inter-agent delay interrupted.");
                    }
                }
            } catch (Exception ex) {
                log.error("{} agent failed during review execution: {}", role, ex.getMessage());
                completedReports.add(new ReviewReport(
                        String.format("%s agent failed to complete its review.", role), 0, new ArrayList<>()));
            }
        }

        List<ReviewComment> combinedComments = completedReports.stream()
                .flatMap(report -> report.getComments() != null ? report.getComments().stream() : Stream.empty())
                .collect(Collectors.toList());

        // Calculate average score (only considering successful reports with a score > 0)
        long count = completedReports.stream().filter(r -> r.getOverallScore() > 0).count();
        int avgScore = count > 0
                ? (int) (completedReports.stream().filter(r -> r.getOverallScore() > 0).mapToInt(ReviewReport::getOverallScore).sum() / count)
                : 0;

        // Generate unified executive summary (wait before this call too)
        log.info("Waiting {}ms before summarization call...", INTER_AGENT_DELAY_MS);
        try {
            Thread.sleep(INTER_AGENT_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        log.info("Aggregating {} agent reports into a final executive summary...", completedReports.size());
        String finalSummary = geminiClient.summarizeReviews(completedReports);

        log.info("Sequential Multi-Agent code review completed. Average Score: {}/100. Total Issues flagged: {}",
                avgScore, combinedComments.size());

        return new ReviewReport(finalSummary, avgScore, combinedComments);
    }
}
