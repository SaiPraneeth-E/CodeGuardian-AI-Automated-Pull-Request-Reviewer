package com.codeguardian.usecases.port.out;

import com.codeguardian.domain.model.CodeSnippet;
import java.util.List;

/**
 * Output port contract for vector store database operations.
 */
public interface VectorStorePort {
    /**
     * Stores a batch of code snippets along with their pre-calculated embeddings.
     *
     * @param snippets list of code snippets
     * @param embeddings list of corresponding vector embeddings
     */
    void upsertSnippets(List<CodeSnippet> snippets, List<float[]> embeddings);

    /**
     * Retrieves code snippets most similar to a query embedding.
     *
     * @param queryEmbedding query vector embedding
     * @param limit maximum number of results to return
     * @return list of similar code snippets
     */
    List<CodeSnippet> findSimilar(float[] queryEmbedding, int limit);

    /**
     * Clears all indexed vectors from the database collection.
     */
    void clearAll();
}
