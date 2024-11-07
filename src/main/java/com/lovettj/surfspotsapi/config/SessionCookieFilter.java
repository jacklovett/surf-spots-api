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
        "/api/user/login",
        "/api/user/register",
        "/api/user/profile",
        "/api/continents/**",
        "/api/countries/**",
        "/api/regions/**",
        "/api/surf-spots/**"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();

        logger.info("Entering SessionCookieFilter for request: {}", requestURI);

        // Check if the endpoint is public and skip session validation if so
        if (isPublicEndpoint(requestURI)) {
            logger.info("Public endpoint, skipping session validation: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

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

    private boolean isPublicEndpoint(String uri) {
        for (String publicPath : PUBLIC_ENDPOINTS) {
            if (pathMatcher.match(publicPath, uri)) {
                return true;
            }
        }
        return false;
    }
}
