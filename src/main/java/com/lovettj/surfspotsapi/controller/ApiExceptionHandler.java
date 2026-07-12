package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.util.SqlExceptionInspection;
import com.lovettj.surfspotsapi.validation.ValidationErrorMessages;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maps API exceptions to {@link ApiResponse}. Controllers should not catch these locally.
 * Unexpected 500s use {@link ApiFailureMessage} on the controller method when present.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        String message = ValidationErrorMessages.firstMethodArgumentNotValidMessage(exception);
        return badRequest(message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return badRequest(ApiErrors.CHECK_INPUT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception) {
        return badRequest(ApiErrors.CHECK_INPUT);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = ValidationErrorMessages.firstConstraintViolationMessage(exception);
        return badRequest(message);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionSystem(
            TransactionSystemException exception, HandlerMethod handlerMethod) {
        ConstraintViolationException constraintViolation =
                ValidationErrorMessages.findConstraintViolationInCauseChain(exception);
        if (constraintViolation == null) {
            logger.warn("Transaction rolled back", exception);
            return internalServerError(handlerMethod);
        }
        String message = ValidationErrorMessages.firstConstraintViolationMessage(constraintViolation);
        return badRequest(message);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException exception, HandlerMethod handlerMethod) {
        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = ApiFailureMessageResolver.resolveServerErrorMessage(handlerMethod);
        }
        int statusCode = exception.getStatusCode().value();
        return ResponseEntity.status(exception.getStatusCode())
                .body(ApiResponse.error(message.trim(), statusCode));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception, HandlerMethod handlerMethod) {
        if (SqlExceptionInspection.isSurfSessionInProgressUniqueViolation(exception)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            ApiErrors.SURF_SESSION_ALREADY_IN_PROGRESS, HttpStatus.CONFLICT.value()));
        }
        if (SqlExceptionInspection.isSurfSessionExternalSyncUniqueViolation(exception)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            ApiErrors.SURF_SESSION_ALREADY_SYNCED, HttpStatus.CONFLICT.value()));
        }
        logger.warn("Data integrity violation", exception);
        return internalServerError(handlerMethod);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception exception, HandlerMethod handlerMethod) {
        ConstraintViolationException constraintViolation =
                ValidationErrorMessages.findConstraintViolationInCauseChain(exception);
        if (constraintViolation != null) {
            String message = ValidationErrorMessages.firstConstraintViolationMessage(constraintViolation);
            return badRequest(message);
        }
        logger.error("Unhandled API exception", exception);
        return internalServerError(handlerMethod);
    }

    private static ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, HttpStatus.BAD_REQUEST.value()));
    }

    private static ResponseEntity<ApiResponse<Void>> internalServerError(HandlerMethod handlerMethod) {
        String message = ApiFailureMessageResolver.resolveServerErrorMessage(handlerMethod);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
