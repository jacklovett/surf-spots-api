package com.lovettj.surfspotsapi.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RateLimiterTests {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    @Test
    void checkRateLimitShouldAllowUpToBucketCap() {
        // LOGIN bucket allows 10 attempts per 15-minute window. All 10 should pass.
        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, "ip=1.2.3.4"));
        }
    }

    @Test
    void checkRateLimitShouldThrow429OnOverflow() {
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, "ip=1.2.3.4");
        }
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, "ip=1.2.3.4"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void checkRateLimitShouldIsolateBuckets() {
        // Same key under different buckets must not share a counter, otherwise a
        // flood of forgot-password attempts would trip the login limiter too.
        for (int i = 0; i < 5; i++) {
            rateLimiter.checkRateLimit(RateLimiter.Bucket.REGISTER, "ip=1.2.3.4");
        }
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, "ip=1.2.3.4"));
    }

    @Test
    void checkRateLimitShouldIsolateKeysWithinBucket() {
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, "ip=1.2.3.4");
        }
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, "ip=5.6.7.8"));
    }

    @Test
    void resetShouldClearCounterForKey() {
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, "ip=1.2.3.4");
        }
        rateLimiter.reset(RateLimiter.Bucket.LOGIN, "ip=1.2.3.4");
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, "ip=1.2.3.4"));
    }

    @Test
    void checkRateLimitShouldNoOpForBlankKey() {
        // A missing key (e.g. email before it's parsed) must not accidentally lock
        // out every caller that happens to supply null.
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, null));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, ""));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, "   "));
    }
}
