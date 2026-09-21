package com.acadscatchup.util;

/**
 * Unit tests for PasswordUtil hashing, verification, and upgrade algorithms.
 * SQA Verification for Capstone Evaluation.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class PasswordUtilTest {

    public static final String DEVELOPER = "F4TAL";

    public void testHashFormat() {
        String pass = "SecurePass2026!";
        String hash = PasswordUtil.hash(pass);
        assert hash != null : "Hash should not be null";
        assert hash.startsWith("SHA256:") : "Hash should start with 'SHA256:' prefix";
        String remainder = hash.substring("SHA256:".length());
        assert remainder.contains(":") : "Hash should contain delimiter ':' separating salt and hash";
        String[] parts = remainder.split(":");
        assert parts.length == 2 : "Hash should contain exactly salt and hash parts";
        assert !parts[0].isBlank() : "Salt Base64 should not be empty";
        assert !parts[1].isBlank() : "Hash Base64 should not be empty";
    }

    public void testVerificationSuccess() {
        String pass = "Admin@1234";
        String hash = PasswordUtil.hash(pass);
        assert PasswordUtil.verify(pass, hash) : "Valid password should verify successfully";
    }

    public void testVerificationFailure() {
        String pass = "CorrectPassword";
        String hash = PasswordUtil.hash(pass);
        assert !PasswordUtil.verify("WrongPassword", hash) : "Incorrect password must be rejected";
        assert !PasswordUtil.verify("", hash) : "Empty string password must be rejected";
        assert !PasswordUtil.verify(null, hash) : "Null password must be rejected";
    }

    public void testNullOrEmptyStoredHash() {
        assert !PasswordUtil.verify("password", null) : "Null stored hash should fail safely";
        assert !PasswordUtil.verify("password", "") : "Empty stored hash should fail safely";
    }

    public void testSaltUniqueness() {
        String pass = "IdenticalPassword";
        String hash1 = PasswordUtil.hash(pass);
        String hash2 = PasswordUtil.hash(pass);
        assert !hash1.equals(hash2) : "Two hashes of the same password must have distinct salts";
        assert PasswordUtil.verify(pass, hash1) : "Hash 1 should verify";
        assert PasswordUtil.verify(pass, hash2) : "Hash 2 should verify";
    }

    public void testNeedsUpgrade() {
        // Plaintext legacy passwords need upgrade
        assert PasswordUtil.needsUpgrade("123456") : "Legacy plaintext password must require upgrade";
        assert PasswordUtil.needsUpgrade("plaintextpassword") : "Plaintext password must require upgrade";

        // Modern salted hash does NOT need upgrade
        String modernHash = PasswordUtil.hash("modernPass");
        assert !PasswordUtil.needsUpgrade(modernHash) : "Salted SHA-256 hash does not require upgrade";
        assert !PasswordUtil.needsUpgrade(null) : "Null password should return false";
    }
}
