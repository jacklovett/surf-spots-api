package com.lovettj.surfspotsapi.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.response.ApiErrors;

/**
 * In-process rate limiter with per-bucket policies.
 *
 * Not suitable for multi-instance deployments. When the API scales horizontally,
 * swap the backing map for Redis / Valkey; the public API does not need to change.
 *
 * Each bucket declares its own window and attempt cap so login (short window,
 * allow occasional typos) can be tuned separately from forgot-password
 * (long window, prevent mass enumeration).
 */
@Component
public class RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    public enum Bucket {
        LOGIN(10, Duration.ofMinutes(15)),
        REGISTER(5, Duration.ofHours(1)),
        FORGOT_PASSWORD(5, Duration.ofHours(1));

        final int maxAttempts;
        final Duration window;

        Bucket(int maxAttempts, Duration window) {
            this.maxAttempts = maxAttempts;
            this.window = window;
        }
    }

    private static final class Entry {
        int attempts;
        Instant expiresAt;

        Entry(Duration window) {
            this.attempts = 1;
            this.expiresAt = Instant.now().plus(window);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, Entry> limiter = new ConcurrentHashMap<>();

    /**
     * Record an attempt and throw 429 if the caller exceeds the bucket's policy.
     *
     * {@code key} is deliberately unstructured so callers can compose it (e.g.
     * {@code "login:ip=1.2.3.4"} vs {@code "login:email=foo@bar.com"}) and limit
     * both dimensions for a single request.
     */
    public void checkRateLimit(Bucket bucket, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        String namespaced = bucket.name() + ":" + key;

        limiter.entrySet().removeIf(e -> e.getValue().isExpired());

        Entry entry = limiter.compute(namespaced, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new Entry(bucket.window);
            }
            existing.attempts++;
            return existing;
        });

        if (entry.attempts > bucket.maxAttempts) {
            logger.warn("Rate limit exceeded for bucket={} key={} attempts={}",
                    bucket, key, entry.attempts);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ApiErrors.TOO_MANY_ATTEMPTS);
        }
    }

    /**
     * Reset the counter for a key after a successful operation so that legitimate
     * users who just mistyped their password once don't burn their quota on the
     * final successful attempt. Call this only after an authenticated success.
     */
    public void reset(Bucket bucket, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        limiter.remove(bucket.name() + ":" + key);
    }
}
