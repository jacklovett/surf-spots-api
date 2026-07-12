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

/**
 * Verifies the signed {@code session} cookie shared with the web app via {@code SESSION_SECRET}.
 * Wire format: {@code <base64-payload>.<base64-hmac>} (split on the last dot).
 *
 * TODO: Decouple API auth from the web app's cookie-signing implementation. Today this verifier
 * matches the signed-cookie format produced by the Remix session storage layer; the API should
 * either own an explicit session-cookie contract both sides implement, or authenticate via a
 * separate API-issued credential instead of parsing the browser session cookie.
 */
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

        int lastDotIndex = cookieValue.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < cookieValue.length() - 1) {
            String payload = cookieValue.substring(0, lastDotIndex);
            String providedSignature = cookieValue.substring(lastDotIndex + 1);
            if (verifyPayloadSignature(payload, providedSignature)) {
                return extractUserIdFromPayload(payload);
            }
        }

        return Optional.empty();
    }

    private boolean verifyPayloadSignature(String payload, String providedSignature) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec key = new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(key);
            byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder()
                    .encodeToString(signatureBytes)
                    .replaceAll("=+$", "");
            String normalizedProvidedSignature = providedSignature.replaceAll("=+$", "");
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    normalizedProvidedSignature.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            logger.error("Failed to verify session cookie signature", exception);
            return false;
        }
    }

    private Optional<String> extractUserIdFromPayload(String payload) {
        try {
            JsonNode root = parseSessionPayload(payload);
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
        } catch (Exception exception) {
            logger.debug("Unable to parse session payload", exception);
            return Optional.empty();
        }
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
        // Only percent-decode values that were URL-encoded (e.g. %2B). Plain Base64 HMAC
        // signatures can contain '+' which URLDecoder would incorrectly turn into a space.
        if (value == null || !value.contains("%")) {
            return value;
        }

        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return value;
        }
    }

    private JsonNode parseSessionPayload(String payload) {
        try {
            byte[] decodedBytes = decodeStandardBase64(payload);
            String escapedJson = new String(decodedBytes, StandardCharsets.UTF_8);
            String json = URLDecoder.decode(percentEncodeSessionJson(escapedJson), StandardCharsets.UTF_8);
            return objectMapper.readTree(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private byte[] decodeStandardBase64(String payload) {
        String paddedPayload = payload;
        int remainder = paddedPayload.length() % 4;
        if (remainder != 0) {
            paddedPayload = paddedPayload + "=".repeat(4 - remainder);
        }
        return Base64.getDecoder().decode(paddedPayload);
    }

    /**
     * Percent-encodes characters outside {@code [\w*+\-./@]} so {@link URLDecoder} can
     * decode session JSON embedded in the signed cookie payload.
     */
    private String percentEncodeSessionJson(String value) {
        StringBuilder result = new StringBuilder(value.length());
        int index = 0;
        while (index < value.length()) {
            char currentChar = value.charAt(index++);
            if (isPercentEncodeLiteral(currentChar)) {
                result.append(currentChar);
                continue;
            }

            int codePoint = currentChar;
            if (codePoint < 256) {
                result.append(String.format("%%%02X", codePoint));
            } else {
                result.append(String.format("%%u%04X", codePoint));
            }
        }
        return result.toString();
    }

    private boolean isPercentEncodeLiteral(char currentChar) {
        return Character.isLetterOrDigit(currentChar)
                || currentChar == '_'
                || currentChar == '*'
                || currentChar == '+'
                || currentChar == '-'
                || currentChar == '.'
                || currentChar == '/'
                || currentChar == '@';
    }
}
