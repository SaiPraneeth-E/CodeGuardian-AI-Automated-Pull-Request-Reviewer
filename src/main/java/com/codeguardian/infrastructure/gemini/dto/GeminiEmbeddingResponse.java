package com.codeguardian.infrastructure.gemini.dto;

import java.util.List;

/**
 * DTO representing the response from generating a single text embedding.
 */
public class GeminiEmbeddingResponse {
    private Embedding embedding;

    public GeminiEmbeddingResponse() {}

    public GeminiEmbeddingResponse(Embedding embedding) {
        this.embedding = embedding;
    }

    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }

    public static class Embedding {
        private List<Double> values;

        public Embedding() {}

        public Embedding(List<Double> values) {
            this.values = values;
        }

        public List<Double> getValues() { return values; }
        public void setValues(List<Double> values) { this.values = values; }

        public float[] getFloatValues() {
            if (values == null) return new float[0];
            float[] arr = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                arr[i] = values.get(i).floatValue();
            }
            return arr;
        }
    }
}
