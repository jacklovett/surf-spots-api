package com.lovettj.surfspotsapi.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Validates and normalizes emergency contact numbers to E.164 for storage.
 */
public final class EmergencyContactPhoneSupport {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private EmergencyContactPhoneSupport() {
    }

    /**
     * Parses and validates the number, then returns canonical E.164 (e.g. +16502530000).
     *
     * @throws ResponseStatusException 400 when the value is present but not a valid number
     */
    public static String normalizeToE164OrThrow(String trimmedPhone) {
        try {
            PhoneNumber parsed = PHONE_UTIL.parse(trimmedPhone, null);
            if (!PHONE_UTIL.isValidNumber(parsed)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Enter a valid phone number including country code.");
            }
            return PHONE_UTIL.format(parsed, PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a valid phone number including country code.");
        }
    }
}
