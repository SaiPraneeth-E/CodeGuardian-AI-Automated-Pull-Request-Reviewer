package com.codeguardian.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service to securely encrypt and decrypt sensitive database columns (e.g. GitHub OAuth Access Tokens)
 * using AES-256 GCM.
 */
@Service
public class TokenEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;

    public TokenEncryptionService(@Value("${codeguardian.security.encryption-secret:default-secret-key-32-chars-length!!}") String secret) {
        try {
            // Hash the input secret using SHA-256 to ensure a secure, 256-bit key
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize TokenEncryptionService key", e);
        }
    }

    /**
     * Encrypts a plaintext string.
     *
     * @param plainText original string
     * @return Base64-encoded encrypted string containing IV + Tag + Ciphertext
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Concatenate IV and CipherText
            byte[] encryptedBytes = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, encryptedBytes, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, encryptedBytes, GCM_IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    /**
     * Decrypts an encrypted Base64 string.
     *
     * @param encryptedText Base64-encoded encrypted string
     * @return decrypted plaintext string
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH);

            int cipherTextLength = encryptedBytes.length - GCM_IV_LENGTH;
            byte[] cipherText = new byte[cipherTextLength];
            System.arraycopy(encryptedBytes, GCM_IV_LENGTH, cipherText, 0, cipherTextLength);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherText);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt token", e);
        }
    }
}
