package com.lovettj.surfspotsapi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

class AuthenticatedUserInterceptorTests {

    private final AuthenticatedUserInterceptor interceptor = new AuthenticatedUserInterceptor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandleShouldAllowAccountDeletePathWhenUriUsesAccountUserIdNotUserId() {
        SecurityContextHolder.getContext().setAuthentication(
                new PreAuthenticatedAuthenticationToken("user-self", null, Collections.emptyList()));

        MockHttpServletRequest request =
                new MockHttpServletRequest(HttpMethod.DELETE.name(), "/api/user/account/other-user");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("accountUserId", "other-user"));

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void preHandleShouldRejectUriUserIdMismatchWhenNotAccountDelete() {
        SecurityContextHolder.getContext().setAuthentication(
                new PreAuthenticatedAuthenticationToken("user-self", null, Collections.emptyList()));

        MockHttpServletRequest request =
                new MockHttpServletRequest(HttpMethod.GET.name(), "/api/trips/user/other-user");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("userId", "other-user"));

        ResponseStatusException thrown =
                assertThrows(
                        ResponseStatusException.class,
                        () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(HttpStatus.FORBIDDEN.value(), thrown.getStatusCode().value());
    }
}
