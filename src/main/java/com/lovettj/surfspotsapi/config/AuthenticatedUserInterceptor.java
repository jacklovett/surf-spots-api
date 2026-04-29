package com.lovettj.surfspotsapi.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticatedUserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }

        String authenticatedUserId = authentication.getPrincipal() instanceof String principal
                ? principal
                : null;

        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            return true;
        }

        assertRequestUserIdMatches(authenticatedUserId, request.getParameter("userId"), "userId");
        assertRequestUserIdMatches(authenticatedUserId, request.getParameter("currentUserId"), "currentUserId");

        @SuppressWarnings("unchecked")
        Map<String, String> uriVariables = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVariables != null) {
            assertRequestUserIdMatches(authenticatedUserId, uriVariables.get("userId"), "userId");
            assertRequestUserIdMatches(authenticatedUserId, uriVariables.get("currentUserId"), "currentUserId");
        }

        return true;
    }

    private void assertRequestUserIdMatches(String authenticatedUserId, String candidate, String fieldName) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }

        if (!authenticatedUserId.equals(candidate)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authenticated user does not match " + fieldName);
        }
    }
}
