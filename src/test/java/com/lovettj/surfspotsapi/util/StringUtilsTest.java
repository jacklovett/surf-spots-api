package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void blankToNullShouldReturnNullForNull() {
        assertNull(StringUtils.blankToNull(null));
    }

    @Test
    void blankToNullShouldReturnNullForBlank() {
        assertNull(StringUtils.blankToNull("   "));
    }

    @Test
    void blankToNullShouldReturnTrimmedValue() {
        assertEquals("Bundoran Peak", StringUtils.blankToNull("  Bundoran Peak  "));
    }
}
