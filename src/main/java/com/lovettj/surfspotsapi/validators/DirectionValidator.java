package com.lovettj.surfspotsapi.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class DirectionValidator implements ConstraintValidator<ValidDirection, String> {

    /** Single cardinal or intercardinal (form options and API payloads). */
    private static final Pattern SINGLE = Pattern.compile("^(N|NE|E|SE|S|SW|W|NW)$");

    /** Range: two compass tokens separated by ASCII hyphen (spaces optional). Not "same token twice" (legacy bug used \\1). */
    private static final Pattern RANGE = Pattern.compile("^(N|NE|E|SE|S|SW|W|NW)\\s*-\\s*(N|NE|E|SE|S|SW|W|NW)$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String trimmed = value.trim();
        return SINGLE.matcher(trimmed).matches() || RANGE.matcher(trimmed).matches();
    }
}
