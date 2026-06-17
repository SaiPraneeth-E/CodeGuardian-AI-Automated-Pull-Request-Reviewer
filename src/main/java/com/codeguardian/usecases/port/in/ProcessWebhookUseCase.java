package com.codeguardian.usecases.port.in;

import com.codeguardian.domain.model.PullRequest;

/**
 * Input port for processing GitHub Webhook Pull Request events.
 */
public interface ProcessWebhookUseCase {
    /**
     * Executes the business logic for a webhook event.
     *
     * @param pullRequest the pull request to process
     * @return the saved/updated pull request
     */
    PullRequest execute(PullRequest pullRequest);
}
