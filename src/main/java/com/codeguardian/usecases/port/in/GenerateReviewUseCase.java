package com.codeguardian.usecases.port.in;

import com.codeguardian.domain.model.ReviewReport;

/**
 * Input port for orchestrating AI Pull Request code reviews.
 */
public interface GenerateReviewUseCase {
    /**
     * Executes the review generation flow for a code diff.
     *
     * @param diffContent the raw git diff string
     * @return the generated review report
     */
    ReviewReport execute(String diffContent);

    /**
     * Executes the review generation flow for a code diff with relevant codebase context.
     *
     * @param diffContent the raw git diff string
     * @param codebaseContext relevant files retrieved from the codebase index
     * @return the generated review report
     */
    ReviewReport execute(String diffContent, String codebaseContext);

    /**
     * Executes the review generation flow for a code diff with relevant codebase context and historical feedback.
     *
     * @param diffContent the raw git diff string
     * @param codebaseContext relevant files retrieved from the codebase index
     * @param repositoryName the name of the repository to fetch historical feedback for
     * @return the generated review report
     */
    ReviewReport execute(String diffContent, String codebaseContext, String repositoryName);
}
