package com.lovettj.surfspotsapi.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

public class SessionCookieFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SessionCookieFilter.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    protected static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/**",
        "/api/continents/**",
        "/api/countries/**",
        "/api/regions/**",
        // Public surf-spots endpoints (GET and filtering POSTs)
        // Exclude /api/surf-spots/management/** which requires authentication
        "/api/surf-spots/region/**",
        "/api/surf-spots/sub-region/**",
        "/api/surf-spots/within-bounds",
        "/api/surf-spots/*",  // GET by slug
        "/api/surf-spots/id/*"};  // GET by id

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        // Strip context path if present (for proper path matching)
        String contextPath = httpRequest.getContextPath();
        String pathToMatch = requestURI;
        if (contextPath != null && !contextPath.isEmpty() && requestURI.startsWith(contextPath)) {
            pathToMatch = requestURI.substring(contextPath.length());
        }

        logger.info("Entering SessionCookieFilter for request: {} {} (matching against: {})", 
                    method, requestURI, pathToMatch);

        // Check if the endpoint is public and skip session validation if so
        if (isPublicEndpoint(pathToMatch, method)) {
            logger.info("Public endpoint, skipping session validation: {} {}", method, pathToMatch);
            chain.doFilter(request, response);
            return;
        }
        
        // Log if endpoint was not recognized as public (for debugging)
        logger.warn("Endpoint not recognized as public: {} {} (checked path: {})", 
                    method, requestURI, pathToMatch);

        // Locate the session cookie
        Cookie sessionCookie = findSessionCookie(httpRequest.getCookies());
        if (sessionCookie != null) {
            logger.info("Session cookie found: {}", sessionCookie.getValue());
        } else {
            logger.info("No session cookie found");
        }

        // Validate the session cookie structure
        if (sessionCookie != null && validateSessionCookieFormat(sessionCookie)) {
            logger.info("Session cookie format validated");

            PreAuthenticatedAuthenticationToken authToken = new PreAuthenticatedAuthenticationToken(
                    "authenticatedUser", null, null
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);

            chain.doFilter(request, response);
        } else {
            logger.error("Session cookie validation failed");
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private Cookie findSessionCookie(Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                logger.info("Checking cookie: {}", cookie.getName());
                if ("session".equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    private boolean validateSessionCookieFormat(Cookie cookie) {
        // Check if the cookie has two parts (payload and signature)
        String[] parts = cookie.getValue().split("\\.");
        return parts.length == 2;  // Valid if two parts exist
    }

    private boolean isPublicEndpoint(String uri, String method) {
        // Check if endpoint matches public paths
        for (String publicPath : PUBLIC_ENDPOINTS) {
            if (pathMatcher.match(publicPath, uri)) {
                // For surf-spots POST endpoints, only filtering endpoints are public
                if (uri.startsWith("/api/surf-spots") && "POST".equals(method)) {
                    // POST to region/sub-region/within-bounds are public (filtering)
                    return uri.startsWith("/api/surf-spots/region/") ||
                           uri.startsWith("/api/surf-spots/sub-region/") ||
                           uri.equals("/api/surf-spots/within-bounds");
                }
                return true;
            }
        }
        return false;
    }
}
