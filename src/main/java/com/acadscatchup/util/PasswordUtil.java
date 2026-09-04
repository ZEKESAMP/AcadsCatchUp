package com.acadscatchup.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Enterprise-grade Password Hashing and Verification Utility.
 * Uses SHA-256 with cryptographically secure random salts.
 * Supports backward-compatible transparent password migration from legacy plaintext.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class PasswordUtil {

    public static final String DEVELOPER = "F4TAL";

    private static final String PREFIX = "SHA256:";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a salted SHA-256 hash formatted as "SHA256:<salt_b64>:<hash_b64>".
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null) return "";
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = computeHash(plainPassword, salt);
        return PREFIX + saltB64 + ":" + hashB64;
    }

    /**
     * Verifies a candidate plain password against a stored password string.
     * Transparently handles both modern hashed passwords and legacy plaintext passwords.
     */
    public static boolean verify(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) return false;

        // Modern Salted SHA-256 format
        if (storedPassword.startsWith(PREFIX)) {
            String remainder = storedPassword.substring(PREFIX.length());
            int colonIdx = remainder.indexOf(':');
            if (colonIdx == -1) return false;
            String saltB64 = remainder.substring(0, colonIdx);
            String expectedHashB64 = remainder.substring(colonIdx + 1);

            try {
                byte[] salt = Base64.getDecoder().decode(saltB64);
                String actualHashB64 = computeHash(plainPassword, salt);
                return MessageDigest.isEqual(
                        expectedHashB64.getBytes(StandardCharsets.UTF_8),
                        actualHashB64.getBytes(StandardCharsets.UTF_8)
                );
            } catch (Exception e) {
                return false;
            }
        }

        // Legacy Plaintext fallback for existing test accounts
        return storedPassword.equals(plainPassword);
    }

    /**
     * Checks if the stored password requires upgrading to a salted hash.
     */
    public static boolean needsUpgrade(String storedPassword) {
        return storedPassword != null && !storedPassword.startsWith(PREFIX);
    }

    private static String computeHash(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
