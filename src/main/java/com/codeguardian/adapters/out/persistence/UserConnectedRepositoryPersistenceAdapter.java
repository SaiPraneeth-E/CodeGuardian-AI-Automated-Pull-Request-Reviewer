package com.codeguardian.adapters.out.persistence;

import com.codeguardian.domain.model.UserConnectedRepository;
import com.codeguardian.usecases.port.out.UserConnectedRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserConnectedRepositoryPersistenceAdapter implements UserConnectedRepositoryPort {

    private final SpringDataUserConnectedRepository springDataRepository;

    public UserConnectedRepositoryPersistenceAdapter(SpringDataUserConnectedRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public UserConnectedRepository save(UserConnectedRepository repository) {
        UserConnectedRepositoryJpaEntity entity = mapToJpaEntity(repository);
        UserConnectedRepositoryJpaEntity savedEntity = springDataRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<UserConnectedRepository> findById(UUID id) {
        return springDataRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public List<UserConnectedRepository> findByUserId(UUID userId) {
        return springDataRepository.findByUserId(userId).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserConnectedRepository> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId) {
        return springDataRepository.findByUserIdAndGithubRepoId(userId, githubRepoId).map(this::mapToDomain);
    }

    @Override
    public Optional<UserConnectedRepository> findByOwnerAndName(String owner, String name) {
        return springDataRepository.findByOwnerAndName(owner, name).map(this::mapToDomain);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }

    private UserConnectedRepositoryJpaEntity mapToJpaEntity(UserConnectedRepository domain) {
        if (domain == null) return null;
        return UserConnectedRepositoryJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .githubRepoId(domain.getGithubRepoId())
                .name(domain.getName())
                .owner(domain.getOwner())
                .webhookId(domain.getWebhookId())
                .webhookSecret(domain.getWebhookSecret())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private UserConnectedRepository mapToDomain(UserConnectedRepositoryJpaEntity entity) {
        if (entity == null) return null;
        return UserConnectedRepository.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .githubRepoId(entity.getGithubRepoId())
                .name(entity.getName())
                .owner(entity.getOwner())
                .webhookId(entity.getWebhookId())
                .webhookSecret(entity.getWebhookSecret())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
