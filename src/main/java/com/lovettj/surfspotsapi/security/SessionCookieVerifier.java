package com.lovettj.surfspotsapi.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SessionCookieVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SessionCookieVerifier.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final String sessionSecret;

    public SessionCookieVerifier(
            ObjectMapper objectMapper,
            @Value("${app.auth.session-secret:${SESSION_SECRET:}}") String sessionSecret) {
        this.objectMapper = objectMapper;
        this.sessionSecret = sessionSecret != null ? sessionSecret : "";
    }

    public Optional<String> verifyAndExtractUserId(String rawCookieValue) {
        if (rawCookieValue == null || rawCookieValue.isBlank()) {
            return Optional.empty();
        }

        if (sessionSecret.isBlank()) {
            logger.warn("Session secret is not configured. Rejecting session cookie.");
            return Optional.empty();
        }

        String decodedCookieValue = decodeCookieValue(rawCookieValue);
        String cookieValue = stripSignedPrefix(decodedCookieValue);
        String[] parts = cookieValue.split("\\.", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        String payload = parts[0];
        String providedSignature = normalizeBase64Url(parts[1]);

        String expectedSignature = normalizeBase64Url(signPayload(payload));
        if (expectedSignature == null) {
            return Optional.empty();
        }

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        return extractUserId(payload);
    }

    private String stripSignedPrefix(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1);
        }

        if (value.startsWith("s:")) {
            return value.substring(2);
        }
        return value;
    }

    private String decodeCookieValue(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            // If decoding fails, keep original value and let signature validation decide.
            return value;
        }
    }

    private String signPayload(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec key = new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(key);
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException ex) {
            logger.error("Failed to verify session cookie signature", ex);
            return null;
        }
    }

    private Optional<String> extractUserId(String payload) {
        try {
            JsonNode root = parsePayloadJson(payload);
            if (root == null) {
                return Optional.empty();
            }

            JsonNode userIdNode = root.path("user").path("id");
            if (userIdNode.isTextual() && !userIdNode.asText().isBlank()) {
                return Optional.of(userIdNode.asText());
            }

            JsonNode directUserIdNode = root.path("userId");
            if (directUserIdNode.isTextual() && !directUserIdNode.asText().isBlank()) {
                return Optional.of(directUserIdNode.asText());
            }

            return Optional.empty();
        } catch (Exception ex) {
            logger.debug("Unable to parse session payload", ex);
            return Optional.empty();
        }
    }

    private JsonNode parsePayloadJson(String payload) {
        // React Router session payloads are typically base64/base64url encoded JSON.
        // Keep parsing tolerant so local dev cookies with different encodings still verify.
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            return objectMapper.readTree(decoded);
        } catch (Exception ignored) {
            // try next parser
        }

        try {
            String padded = payload;
            int remainder = padded.length() % 4;
            if (remainder != 0) {
                padded = padded + "=".repeat(4 - remainder);
            }
            byte[] decoded = Base64.getDecoder().decode(padded);
            return objectMapper.readTree(decoded);
        } catch (Exception ignored) {
            // try next parser
        }

        try {
            return objectMapper.readTree(payload);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeBase64Url(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim()
                .replace('+', '-')
                .replace('/', '_');

        while (normalized.endsWith("=")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}
