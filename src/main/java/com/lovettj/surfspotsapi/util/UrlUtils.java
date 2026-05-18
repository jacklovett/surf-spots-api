package com.lovettj.surfspotsapi.util;

import java.net.URI;

/**
 * Shared URL validation for security (allowlist http/https only).
 * Used by Bean Validation and by services that validate URL lists.
 */
public final class UrlUtils {

    private static final int MAX_URL_LENGTH = 2048;

    private UrlUtils() {
    }

    /**
     * Trims surrounding whitespace and removes trailing {@code /} segments so origins and base URLs
     * compare consistently (e.g. {@code http://app} vs {@code http://app/}).
     *
     * @return {@code null} when {@code value} is null, blank after trim, or empty after slash removal
     */
    public static String stripTrailingSlashes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Returns true if the value is null/blank (optional = valid) or a valid http/https URL (trimmed, max 2048 chars).
     * Returns false for non-blank values that are over-length or not http/https.
     */
    public static boolean isValidHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (value.length() > MAX_URL_LENGTH) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception e) {
            return false;
        }
    }
}
