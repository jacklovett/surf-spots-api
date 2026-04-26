package com.lovettj.surfspotsapi.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for the origins we trust.  building reset-link URLs sent via email
 *
 * The default list matches local dev + the production frontend domain and can
 * be overridden via the {@code CORS_ALLOWED_ORIGINS} env var / {@code cors.allowed-origins} property.
 */
@Component
public class AllowedOrigins {

    private final List<String> origins;

    public AllowedOrigins(
            @Value("${cors.allowed-origins:http://localhost:5173,https://surf-spots-five.vercel.app}") String configuredOrigins) {
        this.origins = Collections.unmodifiableList(Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
    }

    public boolean contains(String origin) {
        return origin != null && origins.contains(origin);
    }

    public List<String> asList() {
        return origins;
    }
}
