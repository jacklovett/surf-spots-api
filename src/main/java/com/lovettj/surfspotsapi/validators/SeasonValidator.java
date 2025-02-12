package com.lovettj.surfspotsapi.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class SeasonValidator implements ConstraintValidator<ValidSeason, String> {

    private static final Pattern SEASON_PATTERN = Pattern.compile(
            "^(January|February|March|April|May|June|July|August|September|October|November|December) - \\1$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && SEASON_PATTERN.matcher(value).matches();
    }
}
