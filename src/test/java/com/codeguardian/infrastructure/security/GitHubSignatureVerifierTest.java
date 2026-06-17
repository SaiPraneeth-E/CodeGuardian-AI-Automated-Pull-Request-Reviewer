package com.codeguardian.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubSignatureVerifierTest {

    private static final String SECRET = "test-webhook-secret-key-999";
    private GitHubSignatureVerifier signatureVerifier;

    @BeforeEach
    void setUp() {
        signatureVerifier = new GitHubSignatureVerifier(SECRET);
    }

    @Test
    void verifySignature_Successful() throws Exception {
        String payload = "{\"action\":\"opened\",\"number\":1}";
        String signatureHeader = "sha256=" + calculateHmac(payload, SECRET);

        boolean result = signatureVerifier.verifySignature(payload, signatureHeader);

        assertTrue(result, "Signature should be valid when secret and payload match.");
    }

    @Test
    void verifySignature_Failed_PayloadMismatch() throws Exception {
        String originalPayload = "{\"action\":\"opened\",\"number\":1}";
        String tamperedPayload = "{\"action\":\"opened\",\"number\":2}";
        String signatureHeader = "sha256=" + calculateHmac(originalPayload, SECRET);

        boolean result = signatureVerifier.verifySignature(tamperedPayload, signatureHeader);

        assertFalse(result, "Signature should be invalid when payload was tampered.");
    }

    @Test
    void verifySignature_Failed_InvalidHeaderFormat() {
        String payload = "{\"action\":\"opened\",\"number\":1}";
        String signatureHeader = "invalid-header-format";

        boolean result = signatureVerifier.verifySignature(payload, signatureHeader);

        assertFalse(result, "Signature should be invalid when format does not start with sha256=.");
    }

    @Test
    void verifySignature_Failed_NullHeader() {
        String payload = "{\"action\":\"opened\",\"number\":1}";

        boolean result = signatureVerifier.verifySignature(payload, null);

        assertFalse(result, "Signature should be invalid when header is null.");
    }

    private String calculateHmac(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(rawHmac);
    }

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
