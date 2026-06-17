package com.codeguardian.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository interface for Pull Request operations.
 */
@Repository
public interface SpringDataPullRequestRepository extends JpaRepository<PullRequestJpaEntity, UUID> {
    /**
     * Resolves a pull request database record based on its unique GitHub Pull Request ID.
     *
     * @param githubPrId the GitHub internal PR ID
     * @return an Optional of the database entity
     */
    Optional<PullRequestJpaEntity> findByGithubPrId(Long githubPrId);
}
