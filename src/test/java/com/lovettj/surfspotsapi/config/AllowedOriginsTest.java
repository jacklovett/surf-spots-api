package com.lovettj.surfspotsapi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class AllowedOriginsTest {

    @Test
    void containsShouldIgnoreTrailingSlashOnRequestOrigin() {
        AllowedOrigins allowedOrigins = new AllowedOrigins("http://localhost:5173");
        assertTrue(allowedOrigins.contains("http://localhost:5173/"));
    }

    @Test
    void resolveTrustedAppOriginShouldMatchRefererWhenOriginAbsent() {
        AllowedOrigins allowedOrigins = new AllowedOrigins("http://localhost:5173");
        Optional<String> resolved =
                allowedOrigins.resolveTrustedAppOrigin(null, "http://localhost:5173/auth/forgot-password");
        assertEquals(Optional.of("http://localhost:5173"), resolved);
    }

    @Test
    void resolveTrustedAppOriginShouldRejectUnknownRefererHost() {
        AllowedOrigins allowedOrigins = new AllowedOrigins("http://localhost:5173");
        assertFalse(allowedOrigins.resolveTrustedAppOrigin(null, "http://evil.example/login").isPresent());
    }
}
