package com.lovettj.surfspotsapi.validation;

import com.lovettj.surfspotsapi.response.ApiErrors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Shared extraction of the first user-facing message from Bean Validation failures.
 * Used by {@link com.lovettj.surfspotsapi.controller.ApiExceptionHandler}.
 */
public final class ValidationErrorMessages {

    private ValidationErrorMessages() {}

    public static String firstMethodArgumentNotValidMessage(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null
                && fieldError.getDefaultMessage() != null
                && !fieldError.getDefaultMessage().isBlank()) {
            return fieldError.getDefaultMessage().trim();
        }

        return exception.getBindingResult().getGlobalErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null && !message.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(ApiErrors.CHECK_INPUT);
    }

    public static String firstConstraintViolationMessage(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(message -> message != null && !message.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(ApiErrors.CHECK_INPUT);
    }

    /** Walks {@code getCause()} for a {@link ConstraintViolationException} (e.g. wrapped by JPA save). */
    public static ConstraintViolationException findConstraintViolationInCauseChain(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException) {
                return constraintViolationException;
            }
            current = current.getCause();
        }
        return null;
    }
}
