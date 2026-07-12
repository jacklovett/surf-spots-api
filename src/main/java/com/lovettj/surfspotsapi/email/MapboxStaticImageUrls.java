package com.lovettj.surfspotsapi.email;

/**
 * Builds Mapbox Static Images API URLs for transactional emails.
 * No SDK required; the browser or mail client loads the image from Mapbox directly.
 */
public final class MapboxStaticImageUrls {

    private MapboxStaticImageUrls() {}

    public static String buildStaticMapImageUrl(
            String accessToken, double latitude, double longitude, int widthPixels, int heightPixels) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        return String.format(
                "https://api.mapbox.com/styles/v1/mapbox/light-v11/static/pin-s+035061(%.6f,%.6f)/%.6f,%.6f,13,0/%dx%d?access_token=%s",
                longitude,
                latitude,
                longitude,
                latitude,
                widthPixels,
                heightPixels,
                accessToken.trim());
    }

    public static String buildMapsLink(double latitude, double longitude) {
        return String.format("https://www.google.com/maps?q=%.6f,%.6f", latitude, longitude);
    }
}
