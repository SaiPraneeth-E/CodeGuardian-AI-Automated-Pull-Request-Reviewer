package com.codeguardian.adapters.in.web;

import com.codeguardian.usecases.service.ReviewChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller exposing endpoints for real-time interactive chat with Gemini reviewer agents.
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewChatController {

    private final ReviewChatService chatService;

    public ReviewChatController(ReviewChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Interactive conversation endpoint about a specific code review.
     *
     * @param reviewId the review ID
     * @param request payload containing user message
     * @return Gemini generated response
     */
    @PostMapping("/{reviewId}/chat")
    public ResponseEntity<ChatResponse> chatAboutReview(
            @PathVariable UUID reviewId,
            @RequestBody ChatRequest request) {
        
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String responseText = chatService.chatAboutReview(reviewId, request.getMessage());
        return ResponseEntity.ok(new ChatResponse(responseText));
    }

    public static class ChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class ChatResponse {
        private String response;

        public ChatResponse(String response) {
            this.response = response;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
    }
}
