package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoordinateDistanceUtilTests {

    @Test
    void distanceKmShouldReturnZeroForIdenticalCoordinates() {
        assertEquals(
                0.0,
                CoordinateDistanceUtil.distanceKm(54.4783, -8.2779, 54.4783, -8.2779),
                0.0001);
    }

    @Test
    void distanceKmShouldMatchKnownShortSeparation() {
        double distanceKm = CoordinateDistanceUtil.distanceKm(54.4783, -8.2779, 54.4820, -8.2779);
        assertTrue(distanceKm > 0.3 && distanceKm < 0.6);
    }
}
