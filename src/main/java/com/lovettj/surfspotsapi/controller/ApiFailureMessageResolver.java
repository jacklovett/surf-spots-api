package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.response.ApiErrors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;

/**
 * Resolves the user-facing message for unexpected API failures on a controller endpoint.
 */
public final class ApiFailureMessageResolver {

    private static final Logger logger = LoggerFactory.getLogger(ApiFailureMessageResolver.class);

    private ApiFailureMessageResolver() {}

    public static String resolveServerErrorMessage(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return ApiErrors.SOMETHING_WENT_WRONG;
        }
        ApiFailureMessage apiFailureMessage = handlerMethod.getMethodAnnotation(ApiFailureMessage.class);
        if (apiFailureMessage == null) {
            logger.warn(
                    "Missing @ApiFailureMessage on {}.{} — falling back to generic server error message",
                    handlerMethod.getBeanType().getSimpleName(),
                    handlerMethod.getMethod().getName());
            return ApiErrors.SOMETHING_WENT_WRONG;
        }
        return ApiErrors.formatErrorMessage(apiFailureMessage.action(), apiFailureMessage.target());
    }
}
