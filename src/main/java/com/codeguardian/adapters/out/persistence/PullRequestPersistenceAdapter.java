package com.codeguardian.adapters.out.persistence;

import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.usecases.port.out.PullRequestRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Persistence Adapter mapping the database repository functions to the domain output port.
 */
@Component
public class PullRequestPersistenceAdapter implements PullRequestRepositoryPort {

    private final SpringDataPullRequestRepository springDataRepository;

    public PullRequestPersistenceAdapter(SpringDataPullRequestRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<PullRequest> findById(java.util.UUID id) {
        return springDataRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public PullRequest save(PullRequest pullRequest) {
        PullRequestJpaEntity entity = mapToJpaEntity(pullRequest);
        PullRequestJpaEntity savedEntity = springDataRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<PullRequest> findByGithubPrId(Long githubPrId) {
        return springDataRepository.findByGithubPrId(githubPrId)
                .map(this::mapToDomain);
    }

    /**
     * Map from Domain Model to JPA Entity
     */
    private PullRequestJpaEntity mapToJpaEntity(PullRequest domain) {
        if (domain == null) {
            return null;
        }
        return PullRequestJpaEntity.builder()
                .id(domain.getId())
                .githubPrId(domain.getGithubPrId())
                .prNumber(domain.getPrNumber())
                .repositoryName(domain.getRepositoryName())
                .owner(domain.getOwner())
                .title(domain.getTitle())
                .state(domain.getState())
                .headSha(domain.getHeadSha())
                .baseSha(domain.getBaseSha())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /**
     * Map from JPA Entity to Domain Model
     */
    private PullRequest mapToDomain(PullRequestJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return PullRequest.builder()
                .id(entity.getId())
                .githubPrId(entity.getGithubPrId())
                .prNumber(entity.getPrNumber())
                .repositoryName(entity.getRepositoryName())
                .owner(entity.getOwner())
                .title(entity.getTitle())
                .state(entity.getState())
                .headSha(entity.getHeadSha())
                .baseSha(entity.getBaseSha())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
