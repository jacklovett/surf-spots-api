package com.lovettj.surfspotsapi.testutil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.Cookie;

public final class SessionTestCookieFactory {

    /**
     * Tests run with the {@code test} profile (Surefire sets {@code spring.profiles.active=test}),
     * loading {@code application-test.yml} from main resources. Keep this value in sync with
     * {@code app.auth.session-secret} there so the API can verify cookies from this factory.
     */
    private static final String SESSION_SECRET = "test-session-secret";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private SessionTestCookieFactory() {}

    public static Cookie createSignedSessionCookie(String userId) {
        try {
            String payloadJson = "{\"user\":{\"id\":\"" + userId + "\"}}";
            String payload = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(SESSION_SECRET.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            String signature = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

            return new Cookie("session", payload + "." + signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create test session cookie", exception);
        }
    }
}
