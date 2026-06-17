package com.codeguardian.usecases.service;

import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.domain.model.ReviewJob;
import com.codeguardian.usecases.port.in.ProcessWebhookUseCase;
import com.codeguardian.usecases.port.out.PullRequestRepositoryPort;
import com.codeguardian.usecases.port.out.ReviewEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Usecase implementation service to handle incoming GitHub Pull Request events.
 */
@Service
public class ProcessWebhookService implements ProcessWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessWebhookService.class);
    private final PullRequestRepositoryPort pullRequestRepository;
    private final ReviewEventPublisherPort eventPublisher;

    public ProcessWebhookService(PullRequestRepositoryPort pullRequestRepository,
                                 ReviewEventPublisherPort eventPublisher) {
        this.pullRequestRepository = pullRequestRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PullRequest execute(PullRequest pullRequest) {
        log.info("Processing webhook pull request event for repository: {}/{} PR #{}",
                pullRequest.getOwner(), pullRequest.getRepositoryName(), pullRequest.getPrNumber());

        if (!pullRequest.isValid()) {
            throw new IllegalArgumentException("Invalid Pull Request payload data received.");
        }

        Optional<PullRequest> existingPrOpt = pullRequestRepository.findByGithubPrId(pullRequest.getGithubPrId());

        PullRequest prToSave;
        if (existingPrOpt.isPresent()) {
            PullRequest existingPr = existingPrOpt.get();
            log.info("Pull Request already exists with ID: {}. Updating state to: {}", existingPr.getId(), pullRequest.getState());
            
            // Re-build domain object preserving original ID and creation timestamp
            prToSave = pullRequest.toBuilder()
                    .id(existingPr.getId())
                    .createdAt(existingPr.getCreatedAt())
                    .build();
        } else {
            log.info("New Pull Request detected. Generating internal ID.");
            prToSave = pullRequest.toBuilder()
                    .id(UUID.randomUUID())
                    .build();
        }

        PullRequest savedPr = pullRequestRepository.save(prToSave);
        log.info("Successfully persisted Pull Request with internal ID: {}", savedPr.getId());

        // Asynchronously publish code review job to Kafka
        ReviewJob job = ReviewJob.builder()
                .pullRequestId(savedPr.getId())
                .owner(savedPr.getOwner())
                .repositoryName(savedPr.getRepositoryName())
                .prNumber(savedPr.getPrNumber())
                .headSha(savedPr.getHeadSha())
                .baseSha(savedPr.getBaseSha())
                .build();
        eventPublisher.publish(job);
        
        return savedPr;
    }
}
