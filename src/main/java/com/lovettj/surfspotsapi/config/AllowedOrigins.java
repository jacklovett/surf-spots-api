package com.lovettj.surfspotsapi.config;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.lovettj.surfspotsapi.util.UrlUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for the origins we trust (CORS, Origin checks, password-reset links).
 *
 * The default list matches local dev + the production frontend domain and can
 * be overridden via the {@code CORS_ALLOWED_ORIGINS} env var / {@code cors.allowed-origins} property.
 */
@Component
public class AllowedOrigins {

    private final List<String> origins;

    public AllowedOrigins(@Value("${cors.allowed-origins}") String configuredOrigins) {
        this.origins = Collections.unmodifiableList(Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
    }

    public boolean contains(String origin) {
        return origin != null && origins.contains(UrlUtils.stripTrailingSlashes(origin));
    }

    public List<String> asList() {
        return origins;
    }

    /**
     * Resolves a trusted app origin for link-building or CSRF-style checks when the browser sends
     * {@code Referer} but not {@code Origin} (common for some navigations). Returns empty when neither
     * header matches the configured allowlist.
     */
    public Optional<String> resolveTrustedAppOrigin(String originHeader, String refererHeader) {
        String trimmedOrigin = UrlUtils.stripTrailingSlashes(originHeader);
        if (trimmedOrigin != null && !trimmedOrigin.isEmpty() && origins.contains(trimmedOrigin)) {
            return Optional.of(trimmedOrigin);
        }
        if (refererHeader == null || refererHeader.isBlank()) {
            return Optional.empty();
        }
        try {
            URI refererUri = URI.create(refererHeader.trim());
            if (refererUri.getScheme() == null || refererUri.getHost() == null) {
                return Optional.empty();
            }
            int port = refererUri.getPort();
            String candidate =
                    refererUri.getScheme() + "://" + refererUri.getHost() + (port > 0 ? ":" + port : "");
            if (origins.contains(candidate)) {
                return Optional.of(candidate);
            }
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
