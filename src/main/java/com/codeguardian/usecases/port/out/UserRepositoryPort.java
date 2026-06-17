package com.codeguardian.usecases.port.out;

import com.codeguardian.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByGithubUserId(Long githubUserId);
    Optional<User> findByLogin(String login);
}
