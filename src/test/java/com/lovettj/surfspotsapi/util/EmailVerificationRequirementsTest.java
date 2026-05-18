package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.response.ApiErrors;

class EmailVerificationRequirementsTest {

    @Test
    void requireVerifiedEmailShouldThrowWhenNotVerified() {
        User user = new User();
        user.setEmailVerified(false);

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> EmailVerificationRequirements.requireVerifiedEmail(user));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(ApiErrors.EMAIL_VERIFICATION_REQUIRED, ex.getReason());
    }

    @Test
    void requireVerifiedEmailShouldNotThrowWhenVerified() {
        User user = new User();
        user.setEmailVerified(true);
        EmailVerificationRequirements.requireVerifiedEmail(user);
    }
}
