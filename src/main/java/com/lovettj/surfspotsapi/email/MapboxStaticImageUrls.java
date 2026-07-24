package com.lovettj.surfspotsapi.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Builds Mapbox Static Images API URLs for transactional emails.
 * No SDK required; the browser or mail client loads the image from Mapbox directly.
 * Coordinate formatting uses {@link Locale#UK} so decimals always use {@code .}
 * regardless of the JVM default locale.
 */
public final class MapboxStaticImageUrls {

    /** Brand teal used for spot pins (matches primary CTA). */
    public static final String PIN_COLOR_SPOT = "035061";

    /** Neutral pin for the recipient's reported location. */
    public static final String PIN_COLOR_USER = "666666";

    private MapboxStaticImageUrls() {}

    public record MapPin(double latitude, double longitude, String colorHexWithoutHash) {
        public MapPin {
            if (colorHexWithoutHash == null || colorHexWithoutHash.isBlank()) {
                colorHexWithoutHash = PIN_COLOR_SPOT;
            } else if (colorHexWithoutHash.startsWith("#")) {
                colorHexWithoutHash = colorHexWithoutHash.substring(1);
            }
        }
    }

    public static String buildStaticMapImageUrl(
            String accessToken, double latitude, double longitude, int widthPixels, int heightPixels) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        return String.format(
                Locale.UK,
                "https://api.mapbox.com/styles/v1/mapbox/light-v11/static/pin-s+%s(%.6f,%.6f)/%.6f,%.6f,13,0/%dx%d?access_token=%s",
                PIN_COLOR_SPOT,
                longitude,
                latitude,
                longitude,
                latitude,
                widthPixels,
                heightPixels,
                accessToken.trim());
    }

    /**
     * Multi-pin static map. Camera uses {@code auto} so all pins fit.
     * Returns null when token is missing or there are no pins.
     */
    public static String buildStaticMapImageUrlWithPins(
            String accessToken, List<MapPin> pins, int widthPixels, int heightPixels) {
        if (accessToken == null || accessToken.isBlank() || pins == null || pins.isEmpty()) {
            return null;
        }
        String overlay =
                pins.stream()
                        .map(
                                pin ->
                                        String.format(
                                                Locale.UK,
                                                "pin-s+%s(%.6f,%.6f)",
                                                pin.colorHexWithoutHash(),
                                                pin.longitude(),
                                                pin.latitude()))
                        .collect(Collectors.joining(","));
        return String.format(
                Locale.UK,
                "https://api.mapbox.com/styles/v1/mapbox/light-v11/static/%s/auto/%dx%d?access_token=%s",
                overlay,
                widthPixels,
                heightPixels,
                accessToken.trim());
    }

    public static String buildMapsLink(double latitude, double longitude) {
        return String.format(Locale.UK, "https://www.google.com/maps?q=%.6f,%.6f", latitude, longitude);
    }

    /** Convenience: user location + spot pins for nearby-travel emails. */
    public static String buildNearbySpotsMapImageUrl(
            String accessToken,
            double userLatitude,
            double userLongitude,
            List<MapPin> spotPins,
            int widthPixels,
            int heightPixels) {
        List<MapPin> allPins = new ArrayList<>();
        allPins.add(new MapPin(userLatitude, userLongitude, PIN_COLOR_USER));
        if (spotPins != null) {
            allPins.addAll(spotPins);
        }
        return buildStaticMapImageUrlWithPins(accessToken, allPins, widthPixels, heightPixels);
    }
}
