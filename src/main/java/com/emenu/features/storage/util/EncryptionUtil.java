package com.emenu.features.storage.util;

import com.emenu.exception.custom.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
@Slf4j
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${app.storage.encryption-key:default-encryption-key-32-chars!}")
    private String encryptionKey;

    /**
     * Encrypt data and return encrypted content
     *
     * @param data The data to encrypt (Base64 encoded)
     * @return EncryptionResult containing encrypted data and IV
     */
    public EncryptionResult encrypt(String data) {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = getSecretKey();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = cipher.doFinal(dataBytes);

            // Return result
            return new EncryptionResult(
                    Base64.getEncoder().encodeToString(encryptedBytes),
                    Base64.getEncoder().encodeToString(iv)
            );
        } catch (Exception e) {
            log.error("Error encrypting data: {}", e.getMessage());
            throw new ValidationException("Failed to encrypt data: " + e.getMessage());
        }
    }

    /**
     * Decrypt data using the provided IV
     *
     * @param encryptedData The encrypted data (Base64 encoded)
     * @param ivBase64      The initialization vector (Base64 encoded)
     * @return Decrypted data
     */
    public String decrypt(String encryptedData, String ivBase64) {
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = getSecretKey();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting data: {}", e.getMessage());
            throw new ValidationException("Failed to decrypt data: " + e.getMessage());
        }
    }

    /**
     * Generate SHA-256 checksum for data
     *
     * @param data The data to hash
     * @return SHA-256 hash as hex string
     */
    public String generateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating checksum: {}", e.getMessage());
            throw new ValidationException("Failed to generate checksum: " + e.getMessage());
        }
    }

    /**
     * Verify checksum matches the data
     */
    public boolean verifyChecksum(byte[] data, String expectedChecksum) {
        String actualChecksum = generateChecksum(data);
        return actualChecksum.equals(expectedChecksum);
    }

    private SecretKey getSecretKey() throws Exception {
        // Derive a 256-bit key from the encryption key using SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        // Use first 32 bytes (256 bits) for AES-256
        keyBytes = Arrays.copyOf(keyBytes, 32);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Result class for encryption operation
     */
    public static class EncryptionResult {
        private final String encryptedData;
        private final String iv;

        public EncryptionResult(String encryptedData, String iv) {
            this.encryptedData = encryptedData;
            this.iv = iv;
        }

        public String getEncryptedData() {
            return encryptedData;
        }

        public String getIv() {
            return iv;
        }
    }
}
