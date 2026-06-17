package com.codeguardian.adapters.out.persistence;

import com.codeguardian.domain.model.PullRequest;
import com.codeguardian.domain.model.Review;
import com.codeguardian.domain.model.ReviewComment;
import com.codeguardian.domain.model.ReviewStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@DataJpaTest
@ActiveProfiles("local")
@Import({ReviewPersistenceAdapter.class, PullRequestPersistenceAdapter.class})
class ReviewPersistenceAdapterTest {

    @Autowired
    private ReviewPersistenceAdapter reviewPersistenceAdapter;

    @Autowired
    private PullRequestPersistenceAdapter pullRequestPersistenceAdapter;

    @Autowired
    private SpringDataReviewRepository reviewRepository;

    @Autowired
    private SpringDataReviewCommentRepository commentRepository;

    @Test
    void testSaveAndRetrieveReview_Success() {
        // 1. Create and persist PullRequest
        UUID prId = UUID.randomUUID();
        PullRequest pr = PullRequest.builder()
                .id(prId)
                .githubPrId(99999L)
                .prNumber(42)
                .repositoryName("guardian-repo")
                .owner("guardian-owner")
                .state("open")
                .headSha("head-123")
                .baseSha("base-123")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        pullRequestPersistenceAdapter.save(pr);

        // 2. Create Review with cascaded comments
        ReviewComment comment1 = new ReviewComment("src/App.java", 12, "BUG", "Null pointer safety issue.");
        ReviewComment comment2 = new ReviewComment("src/Utils.java", 45, "PERFORMANCE", "Use StringBuilder instead of string concatenation.");

        Review review = Review.builder()
                .id(UUID.randomUUID())
                .pullRequestId(prId)
                .status(ReviewStatus.COMPLETED)
                .summary("Overall clean PR with minor performance recommendations.")
                .overallScore(88)
                .createdAt(Instant.now())
                .completedAt(Instant.now())
                .comments(List.of(comment1, comment2))
                .build();

        // 3. Persist review
        Review savedReview = reviewPersistenceAdapter.save(review);

        // 4. Asserts
        Assertions.assertNotNull(savedReview);
        Assertions.assertEquals(review.getId(), savedReview.getId());
        Assertions.assertEquals(ReviewStatus.COMPLETED, savedReview.getStatus());
        Assertions.assertEquals(2, savedReview.getComments().size());

        // 5. Verify direct database records
        Assertions.assertTrue(reviewRepository.findById(review.getId()).isPresent());
        ReviewJpaEntity savedJpa = reviewRepository.findById(review.getId()).get();
        Assertions.assertEquals("Overall clean PR with minor performance recommendations.", savedJpa.getSummary());
        Assertions.assertEquals(88, savedJpa.getOverallScore());

        // Cascade check: comments count in DB
        Assertions.assertEquals(2, savedJpa.getComments().size());
        Assertions.assertEquals(2, commentRepository.count());
    }
}
