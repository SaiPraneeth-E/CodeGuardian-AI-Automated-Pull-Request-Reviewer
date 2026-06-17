package com.codeguardian.usecases.port.out;

import com.codeguardian.domain.model.UserConnectedRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserConnectedRepositoryPort {
    UserConnectedRepository save(UserConnectedRepository repository);
    Optional<UserConnectedRepository> findById(UUID id);
    List<UserConnectedRepository> findByUserId(UUID userId);
    Optional<UserConnectedRepository> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);
    Optional<UserConnectedRepository> findByOwnerAndName(String owner, String name);
    void deleteById(UUID id);
}
