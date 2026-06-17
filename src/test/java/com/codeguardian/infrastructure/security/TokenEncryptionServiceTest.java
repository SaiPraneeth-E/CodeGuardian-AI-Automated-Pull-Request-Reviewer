package com.codeguardian.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenEncryptionServiceTest {

    private TokenEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // Initialize with a test secret key
        encryptionService = new TokenEncryptionService("test-secure-encryption-secret-passphrase");
    }

    @Test
    void testEncryptDecryptSuccess() {
        String originalToken = "gho_1234567890abcdefghijklmnopqrstuvwxyz";

        // Encrypt
        String encrypted = encryptionService.encrypt(originalToken);
        assertNotNull(encrypted);
        assertNotEquals(originalToken, encrypted);

        // Decrypt
        String decrypted = encryptionService.decrypt(encrypted);
        assertNotNull(decrypted);
        assertEquals(originalToken, decrypted);
    }

    @Test
    void testEncryptProducesUniqueOutputsDueToIv() {
        String token = "gho_test_token";

        // Encrypting twice should yield different outputs because of random IVs
        String encrypted1 = encryptionService.encrypt(token);
        String encrypted2 = encryptionService.encrypt(token);

        assertNotNull(encrypted1);
        assertNotNull(encrypted2);
        assertNotEquals(encrypted1, encrypted2);

        // Both should decrypt to the same original token
        assertEquals(token, encryptionService.decrypt(encrypted1));
        assertEquals(token, encryptionService.decrypt(encrypted2));
    }

    @Test
    void testNullAndEmptyHandling() {
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.encrypt(""));
        assertNull(encryptionService.decrypt(null));
        assertNull(encryptionService.decrypt(""));
    }

    @Test
    void testDecryptInvalidDataThrowsException() {
        String invalidCiphertext = "NotABase64String!!";
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(invalidCiphertext));
    }
}
