package com.codeguardian.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data Repository for ReviewJpaEntity access.
 */
@Repository
public interface SpringDataReviewRepository extends JpaRepository<ReviewJpaEntity, UUID> {
}
