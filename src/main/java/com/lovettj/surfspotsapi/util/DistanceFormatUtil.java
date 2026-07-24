package com.lovettj.surfspotsapi.util;

/**
 * Formats distances for email copy using the user's preferred units.
 */
public final class DistanceFormatUtil {

    private static final double FEET_PER_METER = 3.28084;
    private static final double KM_PER_MILE = 1.609344;

    private DistanceFormatUtil() {}

    public static String formatDistanceKm(double distanceKm, String preferredUnits) {
        boolean imperial = preferredUnits != null && preferredUnits.equalsIgnoreCase("imperial");
        if (imperial) {
            double distanceMiles = distanceKm / KM_PER_MILE;
            if (distanceMiles < 1) {
                return Math.round(distanceKm * 1000 * FEET_PER_METER) + " ft";
            }
            return String.format("%.1f mi", distanceMiles);
        }
        if (distanceKm < 1) {
            return Math.round(distanceKm * 1000) + " m";
        }
        return String.format("%.1f km", distanceKm);
    }
}
