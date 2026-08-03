package com.lovettj.surfspotsapi.integration.environmental;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GeoDistanceTest {

    @Test
    void webMercatorToLatLonShouldRoundTripKnownPoint() {
        double longitude = -3.3;
        double latitude = 51.4;
        double xMetres = longitude * 20037508.34 / 180.0;
        double yMetres = Math.log(Math.tan((90.0 + latitude) * Math.PI / 360.0))
                / (Math.PI / 180.0);
        yMetres = yMetres * 20037508.34 / 180.0;

        double[] latLon = GeoDistance.webMercatorToLatLon(xMetres, yMetres);

        assertEquals(latitude, latLon[0], 0.0001);
        assertEquals(longitude, latLon[1], 0.0001);
    }
}
