package com.lovettj.surfspotsapi.integration.environmental;

/**
 * Great-circle distance in metres (WGS84 approximation).
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_METRES = 6_371_000;

    private GeoDistance() {}

    public static double metresBetween(
            double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        double lat1 = Math.toRadians(latitudeA);
        double lat2 = Math.toRadians(latitudeB);
        double deltaLat = Math.toRadians(latitudeB - latitudeA);
        double deltaLon = Math.toRadians(longitudeB - longitudeA);
        double haversine = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return EARTH_RADIUS_METRES * centralAngle;
    }

    /**
     * Convert Web Mercator (EPSG:3857) metres to WGS84 latitude/longitude.
     * @return {@code [latitude, longitude]}
     */
    public static double[] webMercatorToLatLon(double xMetres, double yMetres) {
        double longitude = (xMetres / 20037508.34) * 180.0;
        double latitude = (yMetres / 20037508.34) * 180.0;
        latitude = 180.0 / Math.PI
                * (2.0 * Math.atan(Math.exp(latitude * Math.PI / 180.0)) - Math.PI / 2.0);
        return new double[] {latitude, longitude};
    }
}
