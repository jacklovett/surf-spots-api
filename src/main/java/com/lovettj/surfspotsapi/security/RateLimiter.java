package com.lovettj.surfspotsapi.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long EXPIRY_SECONDS = 3600; // 1 hour

    private final Map<String, RateLimit> limiter = new ConcurrentHashMap<>();

    private static class RateLimit {

        private int attempts;
        private final Instant expiresAt;

        public RateLimit() {
            this.attempts = 1;
            this.expiresAt = Instant.now().plusSeconds(EXPIRY_SECONDS);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public void increment() {
            this.attempts++;
        }
    }

    public void checkRateLimit(String key) {
        // Clean expired entries
        limiter.entrySet().removeIf(entry -> entry.getValue().isExpired());
        // Check rate limit
        RateLimit limit = limiter.computeIfAbsent(key, k -> new RateLimit());

        if (limit.isExpired()) {
            limiter.remove(key);
            limit = new RateLimit();
            limiter.put(key, limit);
        } else if (limit.attempts >= MAX_ATTEMPTS) {
            logger.warn("Rate limit exceeded for key: {}", key);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts");
        } else {
            limit.increment();
            logger.debug("Rate limit increment for key: {}, attempts: {}", key, limit.attempts);
        }
    }
}
