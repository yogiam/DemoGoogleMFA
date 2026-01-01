package com.demo.mfa.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * User model representing a registered user with MFA credentials.
 */
public class User {

    private String email;
    private String passwordHash;
    private String totpSecret;
    private boolean mfaEnabled;

    /**
     * Default constructor for JSON deserialization.
     */
    public User() {
    }

    /**
     * Creates a new user with the specified credentials.
     *
     * @param email       The user's email address
     * @param password    The user's password (will be hashed)
     * @param totpSecret  The Base32-encoded TOTP secret
     * @param mfaEnabled  Whether MFA is enabled for this user
     */
    public User(String email, String password, String totpSecret, boolean mfaEnabled) {
        this.email = email;
        this.passwordHash = hashPassword(password);
        this.totpSecret = totpSecret;
        this.mfaEnabled = mfaEnabled;
    }

    /**
     * Hashes a password using SHA-256.
     * Note: For production use, consider using bcrypt, scrypt, or Argon2.
     *
     * @param password The plain text password
     * @return Base64-encoded SHA-256 hash
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies if the provided password matches the stored hash.
     *
     * @param password The password to verify
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(String password) {
        return passwordHash.equals(hashPassword(password));
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", mfaEnabled=" + mfaEnabled +
                '}';
    }
}
