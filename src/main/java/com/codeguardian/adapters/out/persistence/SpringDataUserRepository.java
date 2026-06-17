package com.codeguardian.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByGithubUserId(Long githubUserId);
    Optional<UserJpaEntity> findByLogin(String login);
}
