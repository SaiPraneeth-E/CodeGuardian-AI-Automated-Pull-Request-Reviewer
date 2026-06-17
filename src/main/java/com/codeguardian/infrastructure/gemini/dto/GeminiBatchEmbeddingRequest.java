package com.codeguardian.infrastructure.gemini.dto;

import java.util.List;

/**
 * DTO representing a batch request to generate multiple text embeddings.
 */
public class GeminiBatchEmbeddingRequest {
    private final List<GeminiEmbeddingRequest> requests;

    public GeminiBatchEmbeddingRequest(List<GeminiEmbeddingRequest> requests) {
        this.requests = requests;
    }

    public List<GeminiEmbeddingRequest> getRequests() { return requests; }
}
