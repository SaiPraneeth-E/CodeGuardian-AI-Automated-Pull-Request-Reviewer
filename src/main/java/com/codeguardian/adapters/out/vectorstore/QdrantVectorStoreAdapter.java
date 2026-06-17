package com.codeguardian.adapters.out.vectorstore;

import com.codeguardian.domain.model.CodeSnippet;
import com.codeguardian.usecases.port.out.VectorStorePort;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter integrating Qdrant Vector Database via gRPC for code chunk semantic search.
 */
@Component
public class QdrantVectorStoreAdapter implements VectorStorePort {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreAdapter.class);

    private final QdrantClient client;
    private final String collectionName;
    private final int vectorSize;

    public QdrantVectorStoreAdapter(
            QdrantClient client,
            @Value("${codeguardian.qdrant.collection-name:codeguardian-codebase}") String collectionName,
            @Value("${codeguardian.qdrant.vector-size:768}") int vectorSize) {
        this.client = client;
        this.collectionName = collectionName;
        this.vectorSize = vectorSize;
    }

    @Override
    public void upsertSnippets(List<CodeSnippet> snippets, List<float[]> embeddings) {
        if (snippets == null || embeddings == null || snippets.size() != embeddings.size()) {
            throw new IllegalArgumentException("Snippets and embeddings size must match.");
        }

        try {
            ensureCollectionExists();

            List<PointStruct> points = new ArrayList<>();
            for (int i = 0; i < snippets.size(); i++) {
                CodeSnippet snippet = snippets.get(i);
                float[] embedding = embeddings.get(i);

                Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
                payload.put("file_path", ValueFactory.value(snippet.getFilePath()));
                payload.put("start_line", ValueFactory.value(snippet.getStartLine()));
                payload.put("end_line", ValueFactory.value(snippet.getEndLine()));
                payload.put("content", ValueFactory.value(snippet.getContent()));

                points.add(PointStruct.newBuilder()
                        .setId(PointIdFactory.id(UUID.randomUUID()))
                        .setVectors(VectorsFactory.vectors(embedding))
                        .putAllPayload(payload)
                        .build());
            }

            log.info("Upserting {} code snippets into Qdrant collection: {}", points.size(), collectionName);
            client.upsertAsync(collectionName, points).get();

        } catch (Exception e) {
            log.error("Failed to upsert code snippets into Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Qdrant upsert failed.", e);
        }
    }

    @Override
    public List<CodeSnippet> findSimilar(float[] queryEmbedding, int limit) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return Collections.emptyList();
        }

        try {
            ensureCollectionExists();

            List<Float> vectorList = new ArrayList<>(queryEmbedding.length);
            for (float val : queryEmbedding) {
                vectorList.add(val);
            }

            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(vectorList)
                    .setLimit(limit)
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();

            log.debug("Querying Qdrant for similar snippets in collection: {}, limit: {}", collectionName, limit);
            List<ScoredPoint> results = client.searchAsync(searchPoints).get();

            return results.stream()
                    .map(point -> {
                        Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = point.getPayloadMap();
                        String filePath = payload.getOrDefault("file_path", ValueFactory.value("")).getStringValue();
                        int startLine = (int) payload.getOrDefault("start_line", ValueFactory.value(0)).getIntegerValue();
                        int endLine = (int) payload.getOrDefault("end_line", ValueFactory.value(0)).getIntegerValue();
                        String content = payload.getOrDefault("content", ValueFactory.value("")).getStringValue();
                        return new CodeSnippet(filePath, startLine, endLine, content);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to query Qdrant similarity search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void clearAll() {
        try {
            if (client.collectionExistsAsync(collectionName).get()) {
                log.info("Deleting existing Qdrant collection: {}", collectionName);
                client.deleteCollectionAsync(collectionName).get();
            }
        } catch (Exception e) {
            log.error("Failed to clear Qdrant collection: {}", e.getMessage(), e);
            throw new RuntimeException("Qdrant collection deletion failed.", e);
        }
    }

    private synchronized void ensureCollectionExists() throws Exception {
        boolean exists = client.collectionExistsAsync(collectionName).get();
        if (!exists) {
            log.info("Qdrant collection '{}' not found. Creating new collection with vector size: {}", collectionName, vectorSize);
            client.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                            .setSize(vectorSize)
                            .setDistance(Distance.Cosine)
                            .build()
            ).get();
        }
    }
}
