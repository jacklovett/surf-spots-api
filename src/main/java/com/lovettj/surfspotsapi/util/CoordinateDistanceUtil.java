package com.lovettj.surfspotsapi.util;

/**
 * Geographic distance helpers aligned with the frontend live-session spot matching radius.
 */
public final class CoordinateDistanceUtil {

    /** Within this distance a GPS session is treated as at the same surf location. */
    public static final double AT_SPOT_RADIUS_KM = 0.5;

    /**
     * When two approved spots fall within {@link #AT_SPOT_RADIUS_KM}, the nearest must lead the
     * next candidate by at least this gap or we omit the name (ambiguous lineup).
     */
    public static final double MIN_CLEAR_SPOT_GAP_KM = 0.15;

    private static final int EARTH_RADIUS_KM = 6371;

    private CoordinateDistanceUtil() {}

    public record CoordinateBoundingBox(
            double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {}

    /** Bounding box for spot lookups around a point (aligned with the frontend nearby-spots helper). */
    public static CoordinateBoundingBox boundingBoxAroundCoordinates(
            double latitude, double longitude, double radiusKm) {
        double latitudeDelta = radiusKm / 111.0;
        double longitudeScale = Math.max(Math.cos(Math.toRadians(latitude)), 0.1);
        double longitudeDelta = radiusKm / (111.0 * longitudeScale);
        return new CoordinateBoundingBox(
                latitude - latitudeDelta,
                latitude + latitudeDelta,
                longitude - longitudeDelta,
                longitude + longitudeDelta);
    }

    public static double distanceKm(double latitudeOne, double longitudeOne, double latitudeTwo, double longitudeTwo) {
        double latitudeDelta = Math.toRadians(latitudeTwo - latitudeOne);
        double longitudeDelta = Math.toRadians(longitudeTwo - longitudeOne);
        double haversine =
                Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                        + Math.cos(Math.toRadians(latitudeOne))
                                * Math.cos(Math.toRadians(latitudeTwo))
                                * Math.sin(longitudeDelta / 2)
                                * Math.sin(longitudeDelta / 2);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return EARTH_RADIUS_KM * centralAngle;
    }
}
