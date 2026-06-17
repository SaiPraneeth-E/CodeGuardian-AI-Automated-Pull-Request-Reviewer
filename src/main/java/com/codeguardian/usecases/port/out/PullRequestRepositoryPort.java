package com.codeguardian.usecases.port.out;

import com.codeguardian.domain.model.PullRequest;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Pull Request persistence operations.
 */
public interface PullRequestRepositoryPort {
    /**
     * Finds a Pull Request by its database ID.
     *
     * @param id the pull request database UUID
     * @return an Optional of the PullRequest domain model
     */
    Optional<PullRequest> findById(UUID id);

    /**
     * Saves or updates a Pull Request.
     *
     * @param pullRequest the pull request to save
     * @return the saved pull request domain object
     */
    PullRequest save(PullRequest pullRequest);

    /**
     * Finds a Pull Request by its GitHub Pull Request ID.
     *
     * @param githubPrId the GitHub pull request ID
     * @return an Optional containing the pull request if found
     */
    Optional<PullRequest> findByGithubPrId(Long githubPrId);
}
