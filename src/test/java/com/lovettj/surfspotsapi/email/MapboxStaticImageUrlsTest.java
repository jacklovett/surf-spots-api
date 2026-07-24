package com.lovettj.surfspotsapi.email;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MapboxStaticImageUrlsTest {

    @Test
    void buildStaticMapImageUrlShouldIncludeSinglePinAndToken() {
        String url =
                MapboxStaticImageUrls.buildStaticMapImageUrl(
                        "test-token", 54.4783, -8.2779, 500, 250);

        assertTrue(url.contains("pin-s+035061(-8.277900,54.478300)"));
        assertTrue(url.contains("/-8.277900,54.478300,13,0/500x250"));
        assertTrue(url.contains("access_token=test-token"));
    }

    @Test
    void buildNearbySpotsMapImageUrlShouldIncludeUserAndSpotPinsWithAutoCamera() {
        String url =
                MapboxStaticImageUrls.buildNearbySpotsMapImageUrl(
                        "test-token",
                        54.48,
                        -8.28,
                        List.of(
                                new MapboxStaticImageUrls.MapPin(
                                        54.49, -8.27, MapboxStaticImageUrls.PIN_COLOR_SPOT)),
                        500,
                        250);

        assertTrue(url.contains("pin-s+666666(-8.280000,54.480000)"));
        assertTrue(url.contains("pin-s+035061(-8.270000,54.490000)"));
        assertTrue(url.contains("/auto/500x250"));
    }

    @Test
    void buildStaticMapImageUrlShouldReturnNullWithoutToken() {
        assertNull(MapboxStaticImageUrls.buildStaticMapImageUrl(null, 1.0, 2.0, 500, 250));
        assertNull(MapboxStaticImageUrls.buildStaticMapImageUrl("  ", 1.0, 2.0, 500, 250));
    }
}
