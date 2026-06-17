package com.codeguardian.infrastructure.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO to deserialize response packets from the Gemini API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
    private List<Candidate> candidates;

    public GeminiResponse() {}

    public GeminiResponse(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public List<Candidate> getCandidates() { return candidates; }
    public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
        private String finishReason;

        public Candidate() {}

        public Candidate(Content content, String finishReason) {
            this.content = content;
            this.finishReason = finishReason;
        }

        public Content getContent() { return content; }
        public void setContent(Content content) { this.content = content; }

        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private List<Part> parts;

        public Content() {}

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() { return parts; }
        public void setParts(List<Part> parts) { this.parts = parts; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private String text;

        public Part() {}

        public Part(String text) {
            this.text = text;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
