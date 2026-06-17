package com.codeguardian.infrastructure.gemini;

import com.codeguardian.domain.model.ReviewReport;
import com.codeguardian.domain.model.ReviewAgentRole;
import com.codeguardian.infrastructure.gemini.dto.GeminiRequest;
import com.codeguardian.infrastructure.gemini.dto.GeminiResponse;
import com.codeguardian.infrastructure.gemini.dto.GeminiReviewReportDto;
import com.codeguardian.infrastructure.gemini.dto.GeminiEmbeddingRequest;
import com.codeguardian.infrastructure.gemini.dto.GeminiEmbeddingResponse;
import com.codeguardian.infrastructure.gemini.dto.GeminiBatchEmbeddingRequest;
import com.codeguardian.infrastructure.gemini.dto.GeminiBatchEmbeddingResponse;
import com.codeguardian.usecases.port.out.GeminiClientPort;
import com.codeguardian.usecases.port.out.EmbeddingClientPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of GeminiClientPort and EmbeddingClientPort to interact with Google's Gemini API.
 */
@Component
public class GeminiClient implements GeminiClientPort, EmbeddingClientPort {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private final Object groqLock = new Object();

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${codeguardian.gemini.api-key}")
    private String apiKey;

    @Value("${codeguardian.gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${codeguardian.gemini.max-retries:3}")
    private int maxRetries;

    @Value("${codeguardian.gemini.backoff-ms:1000}")
    private long backoffMs;

    @Value("${codeguardian.groq.api-key:}")
    private String groqApiKey;

    @Value("${codeguardian.groq.model:llama-3.1-8b-instant}")
    private String groqModel;

    public GeminiClient(RestClient geminiRestClient, ObjectMapper objectMapper) {
        this.restClient = geminiRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReviewReport generateReview(String diffContent) {
        return generateReview(diffContent, null, null);
    }

    @Override
    public ReviewReport generateReview(String diffContent, String codebaseContext) {
        return generateReview(diffContent, codebaseContext, null);
    }

    public ReviewReport generateReview(String diffContent, String codebaseContext, ReviewAgentRole role) {
        return generateReview(diffContent, codebaseContext, null, role);
    }

    @Override
    public ReviewReport generateReview(String diffContent, String codebaseContext, String feedbackContext, ReviewAgentRole role) {
        if (diffContent == null || diffContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Diff content cannot be empty for code review.");
        }

        if (groqApiKey != null && !groqApiKey.trim().isEmpty()) {
            return generateReviewViaGroq(diffContent, codebaseContext, feedbackContext, role);
        }

        GeminiRequest requestPayload = buildRequestPayload(diffContent, codebaseContext, feedbackContext, role);
        int attempts = 0;
        long currentBackoff = backoffMs;

        while (true) {
            attempts++;
            try {
                log.info("Dispatching code review request to Gemini API (Attempt {}/{}) using model: {}",
                        attempts, maxRetries, model);

                ResponseEntity<GeminiResponse> responseEntity = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("generativelanguage.googleapis.com")
                                .path("/v1beta/models/" + model + ":generateContent")
                                .queryParam("key", apiKey)
                                .build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestPayload)
                        .retrieve()
                        .toEntity(GeminiResponse.class);

                GeminiResponse response = responseEntity.getBody();

                if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                    throw new IllegalStateException("Gemini returned an empty response body or candidate list.");
                }

                GeminiResponse.Candidate candidate = response.getCandidates().get(0);
                if (candidate.getContent() == null || candidate.getContent().getParts() == null || candidate.getContent().getParts().isEmpty()) {
                    throw new IllegalStateException("Gemini candidate response lacks content parts.");
                }

                String structuredJsonOutput = candidate.getContent().getParts().get(0).getText();
                log.debug("Successfully received structured response from Gemini API: {}", structuredJsonOutput);

                // Deserialize JSON schema compliant text string into the review report domain
                GeminiReviewReportDto reportDto = objectMapper.readValue(structuredJsonOutput, GeminiReviewReportDto.class);
                return reportDto.toDomain();

            } catch (Exception e) {
                log.warn("Gemini API call failed on attempt {}/{} due to: {}", attempts, maxRetries, e.getMessage());

                if (attempts >= maxRetries) {
                    log.error("Exceeded maximum retry threshold ({}) for Gemini API. Terminating request.", maxRetries);
                    throw new RuntimeException("Gemini API execution failed after multiple retry attempts.", e);
                }

                // Calculate exponential backoff with random jitter to prevent synchronization collisions
                long jitter = (long) (Math.random() * (currentBackoff * 0.5));
                long sleepTime = currentBackoff + jitter;
                log.info("Backing off for {} ms before retry attempt #{}", sleepTime, attempts + 1);

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry waiting state interrupted.", ie);
                }

                currentBackoff *= 2; // Double the backoff duration for the next cycle
            }
        }
    }

    /**
     * Builds request payload specifying senior Java reviewer system rules and JSON schema constraints.
     */
    private GeminiRequest buildRequestPayload(String diffContent, String codebaseContext, String feedbackContext, ReviewAgentRole role) {
        // 1. Setup User content payload containing diff
        String userPrompt = "Review the following git diff for a Pull Request:\n\n" + diffContent;
        if (codebaseContext != null && !codebaseContext.trim().isEmpty()) {
            userPrompt += "\n\nUse the following related repository codebase context to assist in checking dependencies, imports, and utility functions:\n" + codebaseContext;
        }
        if (feedbackContext != null && !feedbackContext.trim().isEmpty()) {
            userPrompt += "\n\nCRITICAL - Learn from the following Historical Developer Feedback on your past reviews:\n" +
                          "If a past suggestion was REJECTED, do NOT make a similar suggestion again.\n" +
                          feedbackContext;
        }
        GeminiRequest.Part userPart = new GeminiRequest.Part(userPrompt);
        GeminiRequest.Content userContent = new GeminiRequest.Content(Collections.singletonList(userPart));

        // 2. Setup system instructions detailing senior reviewer context
        String systemInstructionText = getSystemInstructionForRole(role);
        GeminiRequest.Part systemPart = new GeminiRequest.Part(systemInstructionText);
        GeminiRequest.Content systemInstruction = new GeminiRequest.Content(Collections.singletonList(systemPart));

        // 3. Define schema for structured JSON output
        Map<String, Object> responseSchema = buildResponseSchema();
        GeminiRequest.GenerationConfig generationConfig = new GeminiRequest.GenerationConfig(
                "application/json",
                responseSchema
        );

        return new GeminiRequest(
                Collections.singletonList(userContent),
                systemInstruction,
                generationConfig
        );
    }

    /**
     * Programmatically defines the JSON schema matching Gemini Review structure.
     */
    private Map<String, Object> buildResponseSchema() {
        // Individual Comment properties
        Map<String, Object> commentProperties = new LinkedHashMap<>();
        commentProperties.put("filePath", Map.of("type", "STRING"));
        commentProperties.put("lineNumber", Map.of("type", "INTEGER"));
        commentProperties.put("category", Map.of("type", "STRING", "enum", List.of("BUG", "PERFORMANCE", "SECURITY", "TESTING", "STYLE")));
        commentProperties.put("commentText", Map.of("type", "STRING"));

        Map<String, Object> commentSchema = new LinkedHashMap<>();
        commentSchema.put("type", "OBJECT");
        commentSchema.put("properties", commentProperties);
        commentSchema.put("required", List.of("filePath", "category", "commentText"));

        // Overall Report properties
        Map<String, Object> reportProperties = new LinkedHashMap<>();
        reportProperties.put("summary", Map.of("type", "STRING"));
        reportProperties.put("overallScore", Map.of("type", "INTEGER"));
        reportProperties.put("comments", Map.of("type", "ARRAY", "items", commentSchema));

        Map<String, Object> reportSchema = new LinkedHashMap<>();
        reportSchema.put("type", "OBJECT");
        reportSchema.put("properties", reportProperties);
        reportSchema.put("required", List.of("summary", "overallScore", "comments"));

        return reportSchema;
    }

    private String getSystemInstructionForRole(ReviewAgentRole role) {
        if (role == null) {
            return "You are a Senior Staff Software Engineer and Security Lead specializing in Java 21, Spring Boot 3, clean architecture, clean code standards, performance profiling, OWASP Top 10 vulnerabilities, and JUnit 5 testing practices.\n" +
                   "Your objective is to analyze the provided code diff and return a structured code review.\n" +
                   "Provide constructive summaries and score the PR quality from 1 to 100.\n" +
                   "Only generate comments for lines which possess bugs, security concerns, performance problems, or test deficiencies. Do not comment on correct lines.";
        }
        
        String base = "Your objective is to analyze the provided code diff and return a structured code review.\n" +
                      "Provide constructive summaries and score the PR quality from 1 to 100.\n" +
                      "Only generate comments for lines which possess bugs, security concerns, performance problems, or test deficiencies. Do not comment on correct lines.\n";

        switch (role) {
            case SECURITY:
                return "You are a Senior Application Security Engineer specializing in OWASP Top 10, CWE, and secure coding practices in Java 21 and Spring Boot 3.\n" +
                       "Focus heavily on SQL injection, XSS, insecure direct object references, weak cryptography, and authentication flaws.\n" + base;
            case PERFORMANCE:
                return "You are a Senior Performance Engineer profiling Java 21 backend applications.\n" +
                       "Focus heavily on algorithmic complexity (Big-O), memory leaks, GC overhead, database query N+1 issues, and thread contention.\n" + base;
            case TESTING:
                return "You are a Senior Software Development Engineer in Test (SDET).\n" +
                       "Focus heavily on test coverage gaps, edge cases, negative testing, assertions, and testability of the code using JUnit 5 and Mockito.\n" + base;
            case ARCHITECTURE:
                return "You are a Senior Software Architect focusing on SOLID principles, clean code, design patterns, and domain-driven design.\n" +
                       "Focus heavily on tight coupling, separation of concerns, abstractions, and maintainability.\n" + base;
            default:
                return "You are a Senior Staff Software Engineer.\n" + base;
        }
    }

    @Override
    public String summarizeReviews(List<ReviewReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return "No reviews to summarize.";
        }

        if (groqApiKey != null && !groqApiKey.trim().isEmpty()) {
            return summarizeReviewsViaGroq(reports);
        }

        StringBuilder combinedSummaries = new StringBuilder();
        for (int i = 0; i < reports.size(); i++) {
            combinedSummaries.append("Review ").append(i + 1).append(" Summary:\n")
                             .append(reports.get(i).getSummary()).append("\n\n");
        }

        String userPrompt = "The following are summaries from specialized AI code reviewers (Security, Performance, Testing, Architecture).\n" +
                "Please combine them into a single, cohesive Executive Summary paragraph highlighting the most critical points.\n\n" +
                combinedSummaries.toString();

        GeminiRequest.Part userPart = new GeminiRequest.Part(userPrompt);
        GeminiRequest.Content userContent = new GeminiRequest.Content(Collections.singletonList(userPart));

        GeminiRequest.Part systemPart = new GeminiRequest.Part("You are a Lead Software Architect. Summarize technical reviews concisely.");
        GeminiRequest.Content systemInstruction = new GeminiRequest.Content(Collections.singletonList(systemPart));

        GeminiRequest requestPayload = new GeminiRequest(
                Collections.singletonList(userContent),
                systemInstruction,
                null // no strict json schema for the summary
        );

        int attempts = 0;
        long currentBackoff = backoffMs;

        while (true) {
            attempts++;
            try {
                ResponseEntity<GeminiResponse> responseEntity = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("generativelanguage.googleapis.com")
                                .path("/v1beta/models/" + model + ":generateContent")
                                .queryParam("key", apiKey)
                                .build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestPayload)
                        .retrieve()
                        .toEntity(GeminiResponse.class);

                GeminiResponse response = responseEntity.getBody();
                if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                    return "Summary generation failed (empty response).";
                }
                
                String summary = response.getCandidates().get(0).getContent().getParts().get(0).getText();
                return summary != null ? summary.trim() : "Summary generation returned null.";

            } catch (Exception e) {
                if (attempts >= maxRetries) {
                    log.error("Exceeded maximum retry threshold for summarizing reviews.", e);
                    return "Summary generation failed due to API errors.";
                }
                try {
                    Thread.sleep(currentBackoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "Summary generation interrupted.";
                }
                currentBackoff *= 2;
            }
        }
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty for embedding generation.");
        }
        String embeddingModel = "models/text-embedding-004";
        GeminiEmbeddingRequest payload = new GeminiEmbeddingRequest(embeddingModel, text);

        try {
            ResponseEntity<GeminiEmbeddingResponse> responseEntity = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("generativelanguage.googleapis.com")
                            .path("/v1beta/models/text-embedding-004:embedContent")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(GeminiEmbeddingResponse.class);

            GeminiEmbeddingResponse body = responseEntity.getBody();
            if (body == null || body.getEmbedding() == null) {
                throw new IllegalStateException("Gemini returned empty embedding response.");
            }
            return body.getEmbedding().getFloatValues();
        } catch (Exception e) {
            log.error("Failed to generate text embedding via Gemini API: {}", e.getMessage());
            throw new RuntimeException("Embedding generation failed.", e);
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        String embeddingModel = "models/text-embedding-004";
        List<GeminiEmbeddingRequest> requests = texts.stream()
                .map(t -> new GeminiEmbeddingRequest(embeddingModel, t))
                .collect(Collectors.toList());

        GeminiBatchEmbeddingRequest payload = new GeminiBatchEmbeddingRequest(requests);

        try {
            ResponseEntity<GeminiBatchEmbeddingResponse> responseEntity = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("generativelanguage.googleapis.com")
                            .path("/v1beta/models/text-embedding-004:batchEmbedContents")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(GeminiBatchEmbeddingResponse.class);

            GeminiBatchEmbeddingResponse body = responseEntity.getBody();
            if (body == null || body.getEmbeddings() == null) {
                throw new IllegalStateException("Gemini returned empty batch embedding response.");
            }
            return body.getEmbeddings().stream()
                    .map(GeminiEmbeddingResponse.Embedding::getFloatValues)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to generate batch text embeddings via Gemini API: {}", e.getMessage());
            throw new RuntimeException("Batch embedding generation failed.", e);
        }
    }

    @Override
    public String generateContent(String systemInstructionText, String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("User prompt cannot be empty.");
        }

        if (groqApiKey != null && !groqApiKey.trim().isEmpty()) {
            return generateContentViaGroq(systemInstructionText, userPrompt);
        }

        GeminiRequest.Part userPart = new GeminiRequest.Part(userPrompt);
        GeminiRequest.Content userContent = new GeminiRequest.Content(Collections.singletonList(userPart));

        GeminiRequest.Content systemInstruction = null;
        if (systemInstructionText != null && !systemInstructionText.trim().isEmpty()) {
            GeminiRequest.Part systemPart = new GeminiRequest.Part(systemInstructionText);
            systemInstruction = new GeminiRequest.Content(Collections.singletonList(systemPart));
        }

        GeminiRequest requestPayload = new GeminiRequest(
                Collections.singletonList(userContent),
                systemInstruction,
                null
        );

        int attempts = 0;
        long currentBackoff = backoffMs;

        while (true) {
            attempts++;
            try {
                ResponseEntity<GeminiResponse> responseEntity = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("generativelanguage.googleapis.com")
                                .path("/v1beta/models/" + model + ":generateContent")
                                .queryParam("key", apiKey)
                                .build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestPayload)
                        .retrieve()
                        .toEntity(GeminiResponse.class);

                GeminiResponse response = responseEntity.getBody();
                if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                    throw new IllegalStateException("Gemini returned empty response for content generation.");
                }

                String text = response.getCandidates().get(0).getContent().getParts().get(0).getText();
                return text != null ? text.trim() : "";

            } catch (Exception e) {
                if (attempts >= maxRetries) {
                    log.error("Exceeded maximum retry threshold for content generation.", e);
                    throw new RuntimeException("Gemini content generation failed after multiple attempts.", e);
                }
                try {
                    Thread.sleep(currentBackoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Content generation interrupted.", ie);
                }
                currentBackoff *= 2;
            }
        }
    }

    private ReviewReport generateReviewViaGroq(String diffContent, String codebaseContext, String feedbackContext, ReviewAgentRole role) {
        synchronized (groqLock) {
            String systemInstructionText = getSystemInstructionForRole(role);
            String userPrompt = "Review this PR diff:\n" + diffContent;
            if (codebaseContext != null && !codebaseContext.trim().isEmpty()) {
                userPrompt += "\nContext:\n" + codebaseContext;
            }
            if (feedbackContext != null && !feedbackContext.trim().isEmpty()) {
                userPrompt += "\nPast feedback (avoid repeating REJECTED suggestions):\n" + feedbackContext;
            }

            // Compact JSON schema instruction to minimize token usage
            userPrompt += "\nRespond ONLY with JSON: {\"summary\":\"...\",\"overallScore\":85,\"comments\":[{\"filePath\":\"...\",\"lineNumber\":10,\"category\":\"BUG|PERFORMANCE|SECURITY|TESTING|STYLE\",\"commentText\":\"...\"}]}";

            GroqRequest requestPayload = new GroqRequest(
                    groqModel,
                    List.of(
                            new GroqMessage("system", systemInstructionText),
                            new GroqMessage("user", userPrompt)
                    ),
                    true,
                    2048
            );

            int attempts = 0;
            long currentBackoff = backoffMs;

            while (true) {
                attempts++;
                try {
                    log.info("Dispatching code review request to Groq API (Attempt {}/{}) using model: {}",
                            attempts, maxRetries, groqModel);

                    ResponseEntity<GroqResponse> responseEntity = restClient.post()
                            .uri("https://api.groq.com/openai/v1/chat/completions")
                            .header("Authorization", "Bearer " + groqApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestPayload)
                            .retrieve()
                            .toEntity(GroqResponse.class);

                    GroqResponse response = responseEntity.getBody();
                    if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                        throw new IllegalStateException("Groq returned empty response body or choices.");
                    }

                    String structuredJsonOutput = response.getChoices().get(0).getMessage().getContent();
                    log.debug("Successfully received structured response from Groq API: {}", structuredJsonOutput);

                    GeminiReviewReportDto reportDto = objectMapper.readValue(structuredJsonOutput, GeminiReviewReportDto.class);
                    return reportDto.toDomain();

                } catch (Exception e) {
                    log.warn("Groq API call failed on attempt {}/{} due to: {}", attempts, maxRetries, e.getMessage());

                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        long sleepTime = 61000;
                        if (e.getMessage().contains("try again in ")) {
                            try {
                                String msg = e.getMessage();
                                int idx = msg.indexOf("try again in ");
                                int spaceIdx = msg.indexOf("s", idx + 13);
                                if (spaceIdx > idx) {
                                    String secsStr = msg.substring(idx + 13, spaceIdx + 1).trim();
                                    String unit = "s";
                                    if (secsStr.endsWith("ms")) {
                                        unit = "ms";
                                    }
                                    secsStr = secsStr.replaceAll("[^0-9.]", "");
                                    double value = Double.parseDouble(secsStr);
                                    if ("ms".equals(unit)) {
                                        sleepTime = (long) value + 500;
                                    } else {
                                        sleepTime = (long) (value * 1000) + 2000;
                                    }
                                }
                            } catch (Exception parseEx) {
                                log.warn("Failed to parse Groq retry-after time, defaulting to 61s", parseEx);
                            }
                        }
                        log.info("Detected Groq 429 Rate Limit. Sleeping for {} ms to reset token quota...", sleepTime);
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Retry waiting state interrupted.", ie);
                        }
                    } else {
                        if (attempts >= maxRetries) {
                            log.error("Exceeded maximum retry threshold ({}) for Groq API. Terminating request.", maxRetries);
                            throw new RuntimeException("Groq API execution failed after multiple retry attempts.", e);
                        }

                        long jitter = (long) (Math.random() * (currentBackoff * 0.5));
                        long sleepTime = currentBackoff + jitter;
                        log.info("Backing off for {} ms before retry attempt #{}", sleepTime, attempts + 1);

                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Retry waiting state interrupted.", ie);
                        }

                        currentBackoff *= 2;
                    }
                }
            }
        }
    }

    private String summarizeReviewsViaGroq(List<ReviewReport> reports) {
        StringBuilder combinedSummaries = new StringBuilder();
        for (int i = 0; i < reports.size(); i++) {
            combinedSummaries.append("Review ").append(i + 1).append(" Summary:\n")
                             .append(reports.get(i).getSummary()).append("\n\n");
        }

        String userPrompt = "The following are summaries from specialized AI code reviewers (Security, Performance, Testing, Architecture).\n" +
                "Please combine them into a single, cohesive Executive Summary paragraph highlighting the most critical points.\n\n" +
                combinedSummaries.toString();

        return generateContentViaGroq("You are a Lead Software Architect. Summarize technical reviews concisely.", userPrompt);
    }

    private String generateContentViaGroq(String systemInstructionText, String userPrompt) {
        synchronized (groqLock) {
            GroqRequest requestPayload = new GroqRequest(
                    groqModel,
                    List.of(
                            new GroqMessage("system", systemInstructionText),
                            new GroqMessage("user", userPrompt)
                    ),
                    false,
                    512
            );

            int attempts = 0;
            long currentBackoff = backoffMs;

            while (true) {
                attempts++;
                try {
                    ResponseEntity<GroqResponse> responseEntity = restClient.post()
                            .uri("https://api.groq.com/openai/v1/chat/completions")
                            .header("Authorization", "Bearer " + groqApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestPayload)
                            .retrieve()
                            .toEntity(GroqResponse.class);

                    GroqResponse response = responseEntity.getBody();
                    if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                        throw new IllegalStateException("Groq returned empty response for content generation.");
                    }

                    String text = response.getChoices().get(0).getMessage().getContent();
                    return text != null ? text.trim() : "";

                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        long sleepTime = 61000;
                        if (e.getMessage().contains("try again in ")) {
                            try {
                                String msg = e.getMessage();
                                int idx = msg.indexOf("try again in ");
                                int spaceIdx = msg.indexOf("s", idx + 13);
                                if (spaceIdx > idx) {
                                    String secsStr = msg.substring(idx + 13, spaceIdx + 1).trim();
                                    String unit = "s";
                                    if (secsStr.endsWith("ms")) {
                                        unit = "ms";
                                    }
                                    secsStr = secsStr.replaceAll("[^0-9.]", "");
                                    double value = Double.parseDouble(secsStr);
                                    if ("ms".equals(unit)) {
                                        sleepTime = (long) value + 500;
                                    } else {
                                        sleepTime = (long) (value * 1000) + 2000;
                                    }
                                }
                            } catch (Exception parseEx) {
                                log.warn("Failed to parse Groq retry-after time in content generation, defaulting to 61s", parseEx);
                            }
                        }
                        log.info("Detected Groq 429 Rate Limit in content generation. Sleeping for {} ms...", sleepTime);
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Retry waiting state interrupted.", ie);
                        }
                    } else {
                        if (attempts >= maxRetries) {
                            log.error("Exceeded maximum retry threshold for Groq content generation.", e);
                            throw new RuntimeException("Groq content generation failed after multiple attempts.", e);
                        }
                        try {
                            Thread.sleep(currentBackoff);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Content generation interrupted.", ie);
                        }
                        currentBackoff *= 2;
                    }
                }
            }
        }
    }

    public static class GroqRequest {
        private String model;
        private List<GroqMessage> messages;
        private Map<String, Object> responseFormat;
        @JsonProperty("max_tokens")
        private Integer maxTokens;

        public GroqRequest() {}

        public GroqRequest(String model, List<GroqMessage> messages, boolean jsonMode, int maxTokens) {
            this.model = model;
            this.messages = messages;
            this.maxTokens = maxTokens;
            if (jsonMode) {
                this.responseFormat = Map.of("type", "json_object");
            }
        }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public List<GroqMessage> getMessages() { return messages; }
        public void setMessages(List<GroqMessage> messages) { this.messages = messages; }

        @JsonProperty("response_format")
        public Map<String, Object> getResponseFormat() { return responseFormat; }
        public void setResponseFormat(Map<String, Object> responseFormat) { this.responseFormat = responseFormat; }

        @JsonProperty("max_tokens")
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    }

    public static class GroqMessage {
        private String role;
        private String content;

        public GroqMessage() {}

        public GroqMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroqResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() { return choices; }
        public void setChoices(List<Choice> choices) { this.choices = choices; }

        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            private Message message;

            public Message getMessage() { return message; }
            public void setMessage(Message message) { this.message = message; }
        }

        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {
            private String content;

            public String getContent() { return content; }
            public void setContent(String content) { this.content = content; }
        }
    }
}
