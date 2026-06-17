package com.codeguardian.infrastructure.gemini.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO to serialize requests to the Gemini API.
 */
public class GeminiRequest {
    private List<Content> contents;
    private Content systemInstruction;
    private GenerationConfig generationConfig;

    public GeminiRequest() {}

    public GeminiRequest(List<Content> contents, Content systemInstruction, GenerationConfig generationConfig) {
        this.contents = contents;
        this.systemInstruction = systemInstruction;
        this.generationConfig = generationConfig;
    }

    public List<Content> getContents() { return contents; }
    public void setContents(List<Content> contents) { this.contents = contents; }

    public Content getSystemInstruction() { return systemInstruction; }
    public void setSystemInstruction(Content systemInstruction) { this.systemInstruction = systemInstruction; }

    public GenerationConfig getGenerationConfig() { return generationConfig; }
    public void setGenerationConfig(GenerationConfig generationConfig) { this.generationConfig = generationConfig; }

    public static class Content {
        private List<Part> parts;

        public Content() {}

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() { return parts; }
        public void setParts(List<Part> parts) { this.parts = parts; }
    }

    public static class Part {
        private String text;

        public Part() {}

        public Part(String text) {
            this.text = text;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class GenerationConfig {
        private String responseMimeType;
        private Map<String, Object> responseSchema;

        public GenerationConfig() {}

        public GenerationConfig(String responseMimeType, Map<String, Object> responseSchema) {
            this.responseMimeType = responseMimeType;
            this.responseSchema = responseSchema;
        }

        public String getResponseMimeType() { return responseMimeType; }
        public void setResponseMimeType(String responseMimeType) { this.responseMimeType = responseMimeType; }

        public Map<String, Object> getResponseSchema() { return responseSchema; }
        public void setResponseSchema(Map<String, Object> responseSchema) { this.responseSchema = responseSchema; }
    }
}
