package com.codeguardian.adapters.out.persistence;

import com.codeguardian.domain.model.User;
import com.codeguardian.usecases.port.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository springDataRepository;

    public UserPersistenceAdapter(SpringDataUserRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapToJpaEntity(user);
        UserJpaEntity savedEntity = springDataRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByGithubUserId(Long githubUserId) {
        return springDataRepository.findByGithubUserId(githubUserId).map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return springDataRepository.findByLogin(login).map(this::mapToDomain);
    }

    private UserJpaEntity mapToJpaEntity(User domain) {
        if (domain == null) return null;
        return UserJpaEntity.builder()
                .id(domain.getId())
                .githubUserId(domain.getGithubUserId())
                .login(domain.getLogin())
                .email(domain.getEmail())
                .avatarUrl(domain.getAvatarUrl())
                .oauthTokenEncrypted(domain.getOauthTokenEncrypted())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private User mapToDomain(UserJpaEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .githubUserId(entity.getGithubUserId())
                .login(entity.getLogin())
                .email(entity.getEmail())
                .avatarUrl(entity.getAvatarUrl())
                .oauthTokenEncrypted(entity.getOauthTokenEncrypted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
