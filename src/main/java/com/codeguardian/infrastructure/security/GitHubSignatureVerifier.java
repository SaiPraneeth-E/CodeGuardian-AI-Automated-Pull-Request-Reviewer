package com.codeguardian.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility component to verify GitHub Webhook payloads using HMAC-SHA256 signatures.
 */
@Component
public class GitHubSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(GitHubSignatureVerifier.class);
    private final String webhookSecret;
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    public GitHubSignatureVerifier(@Value("${codeguardian.github.webhook-secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    /**
     * Verifies the authenticity of the incoming GitHub webhook payload.
     *
     * @param payload         the raw body payload of the request
     * @param signatureHeader the X-Hub-Signature-256 header sent by GitHub
     * @return true if the signature is valid, false otherwise
     */
    public boolean verifySignature(String payload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            log.warn("Signature header is missing or does not start with 'sha256='");
            return false;
        }

        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            log.error("GitHub webhook secret is not configured in application properties.");
            return false;
        }

        String receivedSignature = signatureHeader.substring(7); // Extract signature after 'sha256='

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = bytesToHex(rawHmac);

            // Use constant-time comparison to protect against timing attacks
            return MessageDigest.isEqual(
                    receivedSignature.getBytes(StandardCharsets.UTF_8),
                    calculatedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC-SHA256 signature", e);
            return false;
        }
    }

    /**
     * Converts a byte array to its hex string representation.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
