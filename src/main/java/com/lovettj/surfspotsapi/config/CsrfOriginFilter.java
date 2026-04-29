package com.lovettj.surfspotsapi.config;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * CSRF defence for the cookie-authenticated API.
 *
 * Spring Security's synchronizer token is disabled because browser clients live on
 * a separate origin and call this API with credentials. OWASP recommends validating
 * the source origin for state-changing requests as defence in depth alongside
 * SameSite cookies and strict CORS.
 *
 * Safe methods (GET / HEAD / OPTIONS) are not filtered.
 */
@RequiredArgsConstructor
public class CsrfOriginFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CsrfOriginFilter.class);

    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name());

    private final AllowedOrigins allowedOrigins;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (shouldSkip(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String origin = httpRequest.getHeader("Origin");
        String referer = httpRequest.getHeader("Referer");

        if (hasTrustedSource(origin, referer)) {
            chain.doFilter(request, response);
            return;
        }

        logger.warn("CSRF block: {} {} origin={} referer={}",
                httpRequest.getMethod(), httpRequest.getRequestURI(), origin, referer);

        httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        httpResponse.setContentType("application/json");
        objectMapper.writeValue(httpResponse.getWriter(),
                ApiResponse.error(ApiErrors.INVALID_ORIGIN, HttpServletResponse.SC_FORBIDDEN));
    }

    private boolean shouldSkip(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod());
    }

    private boolean hasTrustedSource(String origin, String referer) {
        if (allowedOrigins.contains(origin)) {
            return true;
        }
        if (referer != null && !referer.isBlank()) {
            for (String trusted : allowedOrigins.asList()) {
                if (referer.startsWith(trusted + "/") || referer.equals(trusted)) {
                    return true;
                }
            }
        }
        return false;
    }
}
