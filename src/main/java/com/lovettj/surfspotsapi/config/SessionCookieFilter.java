package com.lovettj.surfspotsapi.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
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
        "/api/surf-spots/region-id/**",
        "/api/surf-spots/sub-region/**",
        "/api/surf-spots/within-bounds",
        "/api/surf-spots/*",
        "/api/surf-spots/id/*"};

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        // Strip context path if present (for proper path matching)
        String contextPath = httpRequest.getContextPath();
        String pathToMatch = requestURI;
        if (contextPath != null && !contextPath.isEmpty() && requestURI.startsWith(contextPath)) {
            pathToMatch = requestURI.substring(contextPath.length());
        }

        logger.debug("SessionCookieFilter: {} {}", method, pathToMatch);

        boolean isPublic = isPublicEndpoint(pathToMatch, method);
        Cookie sessionCookie = findSessionCookie(httpRequest.getCookies());
        if (sessionCookie != null) {
            if (validateSessionCookieFormat(sessionCookie)) {
                PreAuthenticatedAuthenticationToken authToken = new PreAuthenticatedAuthenticationToken(
                        "authenticatedUser", null, null
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                logger.warn("Session cookie format invalid for {} {}", method, pathToMatch);
            }
        }
        chain.doFilter(request, response);
    }

    private Cookie findSessionCookie(Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
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
            boolean matches = pathMatcher.match(publicPath, uri);
            logger.debug("Checking path '{}' against pattern '{}': {}", uri, publicPath, matches);
            
            if (matches) {
                // For surf-spots POST endpoints, only filtering endpoints are public
                if (uri.startsWith("/api/surf-spots") && "POST".equals(method)) {
                    // POST to region/sub-region/within-bounds are public (filtering)
                    boolean isPublicPost = uri.startsWith("/api/surf-spots/region-id/") ||
                           uri.startsWith("/api/surf-spots/sub-region/") ||
                           uri.equals("/api/surf-spots/within-bounds");
                    logger.debug("Surf-spots POST endpoint check: {}", isPublicPost);
                    return isPublicPost;
                }
                logger.debug("Matched public endpoint pattern: {}", publicPath);
                return true;
            }
        }
        logger.debug("No public endpoint pattern matched for: {}", uri);
        return false;
    }
}
