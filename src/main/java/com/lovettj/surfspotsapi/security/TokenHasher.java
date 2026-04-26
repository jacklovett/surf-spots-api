package com.lovettj.surfspotsapi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utilities for opaque bearer-style secrets: generate high-entropy random tokens and
 * produce a deterministic SHA-256 digest (hex) for storage or comparison.
 *
 * Callers decide lifecycle (expiry, single use, where the plaintext is sent). Persist
 * only the digest if you need lookup without storing the raw secret. SHA-256 is a fast
 * digest suitable when the input is already high-entropy; it is not a substitute for
 * password hashing of user-chosen secrets.
 */
public final class TokenHasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private TokenHasher() {}

    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
