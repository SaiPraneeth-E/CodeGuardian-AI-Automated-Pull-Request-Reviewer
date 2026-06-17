package com.codeguardian.adapters.in.web.exception;

import java.time.Instant;

public class ErrorResponseDto {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    public ErrorResponseDto(Instant timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String path;

        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder status(int status) { this.status = status; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder path(String path) { this.path = path; return this; }

        public ErrorResponseDto build() {
            return new ErrorResponseDto(timestamp, status, error, message, path);
        }
    }
}
