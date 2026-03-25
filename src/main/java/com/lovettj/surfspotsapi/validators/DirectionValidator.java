package com.lovettj.surfspotsapi.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class DirectionValidator implements ConstraintValidator<ValidDirection, String> {

    private static final Pattern DIRECTION_PATTERN = Pattern.compile("^(N|NE|E|SE|S|SW|W|NW) - \\1$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return DIRECTION_PATTERN.matcher(value).matches();
    }
}
