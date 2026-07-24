package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DistanceFormatUtilTest {

    @Test
    void testFormatDistanceKmShouldUseMetersBelowOneKilometer() {
        assertEquals("250 m", DistanceFormatUtil.formatDistanceKm(0.25, "metric"));
    }

    @Test
    void testFormatDistanceKmShouldUseFeetBelowOneMileWhenImperial() {
        assertEquals("820 ft", DistanceFormatUtil.formatDistanceKm(0.25, "imperial"));
    }

    @Test
    void testFormatDistanceKmShouldUseMilesAtOneMileWhenImperial() {
        assertEquals("1.0 mi", DistanceFormatUtil.formatDistanceKm(1.609344, "imperial"));
    }
}
