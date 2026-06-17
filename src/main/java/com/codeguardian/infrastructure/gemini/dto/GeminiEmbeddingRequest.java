package com.codeguardian.infrastructure.gemini.dto;

import java.util.Collections;
import java.util.List;

/**
 * DTO representing a request to generate a single text embedding.
 */
public class GeminiEmbeddingRequest {
    private final String model;
    private final Content content;

    public GeminiEmbeddingRequest(String model, String text) {
        this.model = model;
        this.content = new Content(Collections.singletonList(new Part(text)));
    }

    public String getModel() { return model; }
    public Content getContent() { return content; }

    public static class Content {
        private final List<Part> parts;

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() { return parts; }
    }

    public static class Part {
        private final String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() { return text; }
    }
}
