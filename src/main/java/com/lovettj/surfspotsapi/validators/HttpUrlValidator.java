package com.lovettj.surfspotsapi.validators;

import com.lovettj.surfspotsapi.util.UrlUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HttpUrlValidator implements ConstraintValidator<ValidHttpUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return UrlUtils.isValidHttpUrl(value);
    }
}
