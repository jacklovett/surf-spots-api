package com.lovettj.surfspotsapi.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ApiErrorsTest {

    @Test
    void linkSessionsToSpotSuccessMessageShouldReturnNullWhenNoneLinked() {
        assertNull(ApiErrors.linkSessionsToSpotSuccessMessage(0));
    }

    @Test
    void linkSessionsToSpotSuccessMessageShouldReturnSingularWhenOneLinked() {
        assertEquals(ApiErrors.LINK_SESSIONS_ONE_LINKED, ApiErrors.linkSessionsToSpotSuccessMessage(1));
    }

    @Test
    void linkSessionsToSpotSuccessMessageShouldReturnPluralWhenMultipleLinked() {
        assertEquals("3 past sessions linked to this spot.", ApiErrors.linkSessionsToSpotSuccessMessage(3));
    }
}
