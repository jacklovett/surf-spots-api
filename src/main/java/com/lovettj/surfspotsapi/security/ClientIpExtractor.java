package com.lovettj.surfspotsapi.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extracts the caller's public IP address in a proxy-aware way.
 *
 * We prefer {@code X-Forwarded-For} because the API runs behind at least one
 * reverse proxy in every environment (Vercel for the frontend, the cloud load
 * balancer for the API). Only the LEFT-most entry is the client; the rest are
 * proxies. If the header is absent we fall back to the direct peer address so
 * local dev still produces a stable key for the rate limiter.
 *
 * SECURITY: callers must trust this value only for rate-limiting / logging,
 * never for authorization. Any client can forge {@code X-Forwarded-For}.
 */
public final class ClientIpExtractor {

    private ClientIpExtractor() {}

    public static String extract(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma == -1 ? forwarded : forwarded.substring(0, comma)).trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }
}
