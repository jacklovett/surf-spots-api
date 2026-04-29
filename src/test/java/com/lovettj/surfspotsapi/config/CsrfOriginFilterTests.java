package com.lovettj.surfspotsapi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;

class CsrfOriginFilterTests {

    private static final String TRUSTED_ORIGIN = "http://localhost:5173";

    private AllowedOrigins allowedOrigins;
    private CsrfOriginFilter filter;
    private FilterChain chain;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        allowedOrigins = new AllowedOrigins(TRUSTED_ORIGIN);
        filter = new CsrfOriginFilter(allowedOrigins, objectMapper);
        chain = mock(FilterChain.class);
    }

    @Test
    void doFilterShouldAllowSafeMethodsWithoutChecking() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/whatever");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterShouldAllowPostFromTrustedOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", TRUSTED_ORIGIN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterShouldAllowPostWhenRefererIsTrustedAndOriginMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Referer", TRUSTED_ORIGIN + "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterShouldBlockPostFromUntrustedOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "http://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    void doFilterShouldBlockPostWithNoOriginOrReferer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/user/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
    }

    @Test
    void doFilterShouldBlockPostWithRefererPrefixMatchingButNotOnAllowlist() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Referer", TRUSTED_ORIGIN + ".evil.example/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
    }
}
