package com.lovettj.surfspotsapi.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DirectionValidatorTests {

    private DirectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DirectionValidator();
    }

    @Test
    void nullOrBlankShouldBeValid() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }

    @Test
    void singleCardinalsShouldBeValid() {
        assertTrue(validator.isValid("N", null));
        assertTrue(validator.isValid("NE", null));
        assertTrue(validator.isValid("E", null));
        assertTrue(validator.isValid("SE", null));
        assertTrue(validator.isValid("S", null));
        assertTrue(validator.isValid("SW", null));
        assertTrue(validator.isValid("W", null));
        assertTrue(validator.isValid("NW", null));
    }

    @Test
    void rangesShouldBeValid() {
        assertTrue(validator.isValid("NE - SE", null));
        assertTrue(validator.isValid("N - S", null));
        assertTrue(validator.isValid("NW - NW", null));
        assertTrue(validator.isValid("NE-SE", null));
        // Compact hyphen, different endpoints (old regex used \\1 and wrongly rejected these)
        assertTrue(validator.isValid("N-S", null));
        assertTrue(validator.isValid("NE-SW", null));
    }

    @Test
    void invalidDirectionShouldFail() {
        assertFalse(validator.isValid("X", null));
        assertFalse(validator.isValid("NN", null));
        assertFalse(validator.isValid("North", null));
    }

    @Test
    void malformedRangeShouldFail() {
        assertFalse(validator.isValid("NE - SE - NW", null));
        assertFalse(validator.isValid("NE - X", null));
    }
}
