package com.lovettj.surfspotsapi.controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User-facing 500 message for an endpoint when {@link ApiExceptionHandler} handles an unexpected
 * failure. Maps to {@link com.lovettj.surfspotsapi.response.ApiErrors#formatErrorMessage(String, String)}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiFailureMessage {
    /** Verb fragment, e.g. {@code "create"}, {@code "update"}, {@code "load"}. */
    String action();

    /** Optional noun, e.g. {@code "surf spot"}, {@code "trip"}. */
    String target() default "";
}
