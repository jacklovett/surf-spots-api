package com.lovettj.surfspotsapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokenHasherTests {

    @Test
    void generateTokenShouldProduceHighEntropyUrlSafeString() {
        String token = TokenHasher.generateToken();
        assertNotNull(token);
        // 32 random bytes => 43 base64url chars (no padding).
        assertEquals(43, token.length());
        // base64url allows A–Z, a–z, 0–9, '-', '_'. No '+', '/', or '=' means the
        // token can ride in a URL query string without escaping.
        assertTrue(token.matches("[A-Za-z0-9_-]+"),
                "Token must be URL-safe base64 with no padding");
    }

    @Test
    void generateTokenShouldBeUniqueAcrossCalls() {
        String first = TokenHasher.generateToken();
        String second = TokenHasher.generateToken();
        assertNotEquals(first, second);
    }

    @Test
    void hashShouldBeDeterministicAndSha256HexLength() {
        String hashed = TokenHasher.hash("some-token");
        assertEquals(64, hashed.length(), "SHA-256 hex = 32 bytes = 64 chars");
        assertEquals(hashed, TokenHasher.hash("some-token"));
        assertTrue(hashed.matches("[0-9a-f]+"), "Hash must be lowercase hex");
    }

    @Test
    void hashShouldNotEqualInput() {
        String token = TokenHasher.generateToken();
        assertNotEquals(token, TokenHasher.hash(token));
    }

    @Test
    void hashShouldDifferForDifferentInputs() {
        assertFalse(TokenHasher.hash("a").equals(TokenHasher.hash("b")));
    }
}
