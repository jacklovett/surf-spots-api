package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ContestVenueLocationKeyUtilTests {

    @Test
    void testNormalizeLocationKeyShouldLowercaseAndSlugify() {
        String key = ContestVenueLocationKeyUtil.normalizeLocationKey("Punta Roca, La Libertad, El Salvador");
        assertEquals("punta-roca-la-libertad-el-salvador", key);
    }

    @Test
    void testNormalizeLocationKeyShouldTreatEquivalentPunctuationAsSameVenue() {
        String firstKey = ContestVenueLocationKeyUtil.normalizeLocationKey("Saquarema, Rio de Janeiro, Brazil");
        String secondKey = ContestVenueLocationKeyUtil.normalizeLocationKey("Saquarema Rio de Janeiro Brazil");
        assertEquals(firstKey, secondKey);
    }

    @Test
    void testNormalizeLocationKeyShouldRejectBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> ContestVenueLocationKeyUtil.normalizeLocationKey("   "));
    }
}
