package com.codeguardian.domain.exception;

/**
 * Exception thrown when the GitHub webhook signature verification fails.
 */
public class WebhookSecretInvalidException extends RuntimeException {
    public WebhookSecretInvalidException(String message) {
        super(message);
    }
}
