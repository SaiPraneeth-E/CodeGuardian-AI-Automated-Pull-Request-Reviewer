package com.codeguardian.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserConnectedRepository extends JpaRepository<UserConnectedRepositoryJpaEntity, UUID> {
    List<UserConnectedRepositoryJpaEntity> findByUserId(UUID userId);
    Optional<UserConnectedRepositoryJpaEntity> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);
    Optional<UserConnectedRepositoryJpaEntity> findByOwnerAndName(String owner, String name);
}
