package com.example.telemetryservice.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class EncryptionUtility {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionUtility.class);
    private static final String ALGORITHM = "AES";
    private static final String ENCRYPTION_KEY = resolveKey();
    private static final boolean ENCRYPT_DATA_AT_REST = resolveEncryptionEnabled();

    private static String resolveKey() {
        LOGGER.info("Attempting to resolve encryption key");

        String key = System.getProperty("ENCRYPTION_KEY");
        if (key == null) {
            LOGGER.warn("Unable to grab from properties, attempt with environment variables");
            key = System.getenv("ENCRYPTION_KEY");
        }
        if (key == null || key.length() != 32) {
            LOGGER.error("ENCRYPTION_KEY must be set and 32 chars long");
            throw new IllegalStateException("ENCRYPTION_KEY must be set and 32 chars long");
        }

        LOGGER.info("Encryption key successfully resolved");
        return key;
    }

    private static boolean resolveEncryptionEnabled() {
        LOGGER.info("Attempting to resolve ENCRYPT_DATA_AT_REST env var");

        String flag = System.getProperty("ENCRYPT_DATA_AT_REST");
        if (flag == null) {
            LOGGER.warn("Unable to grab from properties, attempt with environment variables");
            flag = System.getenv("ENCRYPT_DATA_AT_REST");
        }
        if(flag == null) {
            LOGGER.info("ENCRYPT_DATA_AT_REST is not set, defaulting to TRUE");
            flag = "true";
        }

        boolean enabled = Boolean.parseBoolean(flag);
        LOGGER.info("Data-at-rest encryption is " +  (enabled ? "ENABLED" : "DISABLED"));

        return enabled;
    }

    public static String encrypt(String raw) {
        if (!ENCRYPT_DATA_AT_REST)
            return raw;

        try {
            SecretKey key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ALGORITHM);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            return Base64.getEncoder().encodeToString(cipher.doFinal(raw.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    public static String decrypt(String encrypted) {
        if (!ENCRYPT_DATA_AT_REST)
            return encrypted;

        try {
            SecretKey key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ALGORITHM);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
        } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }
}
