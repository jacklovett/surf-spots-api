package com.lovettj.surfspotsapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.testutil.SessionTestCookieFactory;

class SessionCookieVerifierTest {

    private static final String SESSION_SECRET = "test-session-secret";
    private static final String USER_ID = "10143c2f-5c2a-4fd1-8045-f46e33a1b36a";

    private SessionCookieVerifier sessionCookieVerifier;

    @BeforeEach
    void setUp() {
        sessionCookieVerifier = new SessionCookieVerifier(new ObjectMapper(), SESSION_SECRET);
    }

    @Test
    void verifyAndExtractUserIdShouldAcceptSignedSessionCookie() {
        String cookieValue = SessionTestCookieFactory.createSignedSessionCookie(USER_ID).getValue();

        Optional<String> userId = sessionCookieVerifier.verifyAndExtractUserId(cookieValue);

        assertTrue(userId.isPresent());
        assertEquals(USER_ID, userId.get());
    }

    @Test
    void verifyAndExtractUserIdShouldAcceptSignatureContainingPlusSign() {
        String cookieWithPlusSignature = null;
        String expectedUserId = null;

        for (int suffix = 0; suffix < 500; suffix++) {
            String candidateUserId = "plus-signature-user-" + suffix;
            String candidateCookie = SessionTestCookieFactory.createSignedSessionCookie(candidateUserId).getValue();
            int signatureStartIndex = candidateCookie.lastIndexOf('.') + 1;
            String signature = candidateCookie.substring(signatureStartIndex);
            if (signature.contains("+")) {
                cookieWithPlusSignature = candidateCookie;
                expectedUserId = candidateUserId;
                break;
            }
        }

        assertNotNull(cookieWithPlusSignature, "Expected to find a signed cookie whose HMAC contains '+'");

        Optional<String> extractedUserId = sessionCookieVerifier.verifyAndExtractUserId(cookieWithPlusSignature);

        assertTrue(extractedUserId.isPresent());
        assertEquals(expectedUserId, extractedUserId.get());
    }
}
