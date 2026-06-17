package com.codeguardian.adapters.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data Repository for ReviewCommentJpaEntity access.
 */
@Repository
public interface SpringDataReviewCommentRepository extends JpaRepository<ReviewCommentJpaEntity, UUID> {

    @Query("SELECT c FROM ReviewCommentJpaEntity c " +
           "JOIN c.review r JOIN r.pullRequest p " +
           "WHERE p.repositoryName = :repositoryName AND c.accepted IS NOT NULL " +
           "ORDER BY c.createdAt DESC")
    List<ReviewCommentJpaEntity> findRecentFeedbackByRepository(
            @Param("repositoryName") String repositoryName, Pageable pageable);

    @Query("SELECT c.agentType, " +
           "SUM(CASE WHEN c.accepted = true THEN 1 ELSE 0 END) * 100.0 / COUNT(c) " +
           "FROM ReviewCommentJpaEntity c " +
           "WHERE c.accepted IS NOT NULL " +
           "GROUP BY c.agentType")
    List<Object[]> getAcceptanceRates();
}
