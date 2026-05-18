package com.lovettj.surfspotsapi.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.lovettj.surfspotsapi.response.ApiErrors;

class EmailLayoutVariablesTest {

    @Test
    void normalizeAppBaseUrlShouldStripTrailingSlashes() {
        assertEquals(
                "https://example.com",
                EmailLayoutVariables.normalizeAppBaseUrl("https://example.com///"));
    }

    @Test
    void normalizeAppBaseUrlShouldThrowSafeMessageWhenMissing() {
        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> EmailLayoutVariables.normalizeAppBaseUrl(null));
        assertEquals(ApiErrors.SOMETHING_WENT_WRONG, thrown.getMessage());

        IllegalStateException thrownBlank =
                assertThrows(IllegalStateException.class, () -> EmailLayoutVariables.normalizeAppBaseUrl("   "));
        assertEquals(ApiErrors.SOMETHING_WENT_WRONG, thrownBlank.getMessage());
    }

    @Test
    void resolveLogoImageUrlShouldPreferOverride() {
        assertEquals(
                "https://cdn.example/logo.png",
                EmailLayoutVariables.resolveLogoImageUrl(
                        "https://cdn.example/logo.png", "https://app.example.com"));
    }

    @Test
    void resolveLogoImageUrlShouldDefaultUnderAppBase() {
        assertEquals(
                "https://app.example.com/images/png/logo.png",
                EmailLayoutVariables.resolveLogoImageUrl("", "https://app.example.com"));
    }
}
