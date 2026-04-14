package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EmergencyContactPhoneSupportTest {

    @Test
    void normalizeToE164OrThrowShouldAcceptValidNumberWithFormatting() {
        assertEquals("+16502530000",
                EmergencyContactPhoneSupport.normalizeToE164OrThrow("+1 650 253 0000"));
    }

    @Test
    void normalizeToE164OrThrowShouldRejectInvalidInput() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> EmergencyContactPhoneSupport.normalizeToE164OrThrow("abc"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
