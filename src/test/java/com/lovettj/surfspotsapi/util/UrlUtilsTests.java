package com.lovettj.surfspotsapi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlUtilsTests {

    @Test
    void testIsValidHttpUrlShouldReturnTrueForNull() {
        assertTrue(UrlUtils.isValidHttpUrl(null));
    }

    @Test
    void testIsValidHttpUrlShouldReturnTrueForBlank() {
        assertTrue(UrlUtils.isValidHttpUrl(""));
        assertTrue(UrlUtils.isValidHttpUrl("   "));
    }

    @Test
    void testIsValidHttpUrlShouldReturnTrueForValidHttps() {
        assertTrue(UrlUtils.isValidHttpUrl("https://example.com"));
        assertTrue(UrlUtils.isValidHttpUrl("https://example.com/path?q=1"));
    }

    @Test
    void testIsValidHttpUrlShouldReturnTrueForValidHttp() {
        assertTrue(UrlUtils.isValidHttpUrl("http://localhost:3000"));
    }

    @Test
    void testIsValidHttpUrlShouldTrimWhitespace() {
        assertTrue(UrlUtils.isValidHttpUrl("  https://example.com  "));
    }

    @Test
    void testIsValidHttpUrlShouldReturnFalseForJavaScriptScheme() {
        assertFalse(UrlUtils.isValidHttpUrl("javascript:alert(1)"));
        assertFalse(UrlUtils.isValidHttpUrl("javascript:void(0)"));
    }

    @Test
    void testIsValidHttpUrlShouldReturnFalseForDataScheme() {
        assertFalse(UrlUtils.isValidHttpUrl("data:text/html,<script>alert(1)</script>"));
    }

    @Test
    void testIsValidHttpUrlShouldReturnFalseForInvalidFormat() {
        assertFalse(UrlUtils.isValidHttpUrl("not-a-url"));
    }

    @Test
    void testIsValidHttpUrlShouldReturnFalseWhenOverMaxLength() {
        String longUrl = "https://example.com/" + "a".repeat(2048);
        assertFalse(UrlUtils.isValidHttpUrl(longUrl));
    }
}
