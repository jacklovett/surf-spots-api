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
        
        // Log all public endpoint patterns for debugging
        logger.debug("Available public endpoint patterns: {}", java.util.Arrays.toString(PUBLIC_ENDPOINTS));

        // Check if the endpoint is public and skip session validation if so
        boolean isPublic = isPublicEndpoint(pathToMatch, method);
        logger.info("Is public endpoint: {} for path: {}", isPublic, pathToMatch);
        
        // Always check for session cookie and set authentication if valid
        // This helps authenticated requests work, but doesn't block unauthenticated ones
        Cookie sessionCookie = findSessionCookie(httpRequest.getCookies());
        if (sessionCookie != null) {
            logger.info("Session cookie found: {}", sessionCookie.getValue());
            
            // Validate the session cookie structure
            if (validateSessionCookieFormat(sessionCookie)) {
                logger.info("Session cookie format validated");

                PreAuthenticatedAuthenticationToken authToken = new PreAuthenticatedAuthenticationToken(
                        "authenticatedUser", null, null
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                logger.warn("Session cookie format invalid, but continuing to let Spring Security handle authorization");
            }
        } else {
            logger.info("No session cookie found, letting Spring Security handle authorization");
        }
        
        // ALWAYS continue the filter chain - NEVER return 401 from this filter
        // Let Spring Security decide if the request should be allowed based on permitAll() configuration
        logger.info("Continuing filter chain for: {} {}", method, pathToMatch);
        chain.doFilter(request, response);
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
            boolean matches = pathMatcher.match(publicPath, uri);
            logger.debug("Checking path '{}' against pattern '{}': {}", uri, publicPath, matches);
            
            if (matches) {
                // For surf-spots POST endpoints, only filtering endpoints are public
                if (uri.startsWith("/api/surf-spots") && "POST".equals(method)) {
                    // POST to region/sub-region/within-bounds are public (filtering)
                    boolean isPublicPost = uri.startsWith("/api/surf-spots/region/") ||
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
