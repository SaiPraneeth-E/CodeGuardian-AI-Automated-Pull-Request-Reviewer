package com.codeguardian.infrastructure.gemini.dto;

import java.util.List;

/**
 * DTO representing the response containing multiple generated embeddings.
 */
public class GeminiBatchEmbeddingResponse {
    private List<GeminiEmbeddingResponse.Embedding> embeddings;

    public GeminiBatchEmbeddingResponse() {}

    public GeminiBatchEmbeddingResponse(List<GeminiEmbeddingResponse.Embedding> embeddings) {
        this.embeddings = embeddings;
    }

    public List<GeminiEmbeddingResponse.Embedding> getEmbeddings() { return embeddings; }
    public void setEmbeddings(List<GeminiEmbeddingResponse.Embedding> embeddings) { this.embeddings = embeddings; }
}
