package com.codeguardian.usecases.port.out;

import java.util.List;

/**
 * Output port contract for generating vector embeddings from text content.
 */
public interface EmbeddingClientPort {
    /**
     * Generates a vector embedding for a single text payload.
     *
     * @param text the content to embed
     * @return the numerical vector representation
     */
    float[] generateEmbedding(String text);

    /**
     * Batch generates vector embeddings for a list of text payloads.
     *
     * @param texts list of contents to embed
     * @return list of vector embeddings
     */
    List<float[]> generateEmbeddings(List<String> texts);
}
