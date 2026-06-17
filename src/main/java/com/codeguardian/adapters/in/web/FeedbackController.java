package com.codeguardian.adapters.in.web;

import com.codeguardian.usecases.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller handling developer feedback endpoints and metric retrieval.
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Endpoint to submit feedback on a generated review comment.
     *
     * @param commentId the internal UUID of the comment.
     * @param accepted true if accepted, false if rejected.
     * @return 200 OK if successfully updated.
     */
    @PostMapping("/{commentId}")
    public ResponseEntity<Void> submitFeedback(
            @PathVariable UUID commentId,
            @RequestParam boolean accepted) {
        
        feedbackService.submitFeedback(commentId, accepted);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint to retrieve acceptance rates for AI review agents.
     *
     * @return a JSON object containing agent roles mapped to their acceptance percentage.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Double>> getFeedbackMetrics() {
        return ResponseEntity.ok(feedbackService.getMetrics());
    }
}
