package com.lovettj.surfspotsapi.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.response.ApiErrors;

/**
 * Central checks for actions that should only run once the account email is verified.
 */
public final class EmailVerificationRequirements {

    private EmailVerificationRequirements() {
    }

    /**
     * @throws ResponseStatusException {@link HttpStatus#FORBIDDEN} when the user has not verified their email
     */
    public static void requireVerifiedEmail(User user) {
        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrors.EMAIL_VERIFICATION_REQUIRED);
        }
    }
}
