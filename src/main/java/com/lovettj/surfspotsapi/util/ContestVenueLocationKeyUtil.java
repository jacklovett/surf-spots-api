package com.lovettj.surfspotsapi.util;

/**
 * Normalizes contest venue location strings into a stable key for upsert and auto-linking.
 */
public final class ContestVenueLocationKeyUtil {

    private ContestVenueLocationKeyUtil() {}

    public static String normalizeLocationKey(String locationName) {
        if (locationName == null || locationName.isBlank()) {
            throw new IllegalArgumentException("locationName is required");
        }
        String lowered = locationName.trim().toLowerCase();
        String alphanumericAndSpaces = lowered.replaceAll("[^a-z0-9\\s]", " ");
        return alphanumericAndSpaces.trim().replaceAll("\\s+", "-");
    }
}
