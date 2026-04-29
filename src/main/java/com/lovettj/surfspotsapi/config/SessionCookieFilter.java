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

import com.lovettj.surfspotsapi.security.SessionCookieVerifier;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

public class SessionCookieFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SessionCookieFilter.class);
    private final SessionCookieVerifier sessionCookieVerifier;

    public SessionCookieFilter(SessionCookieVerifier sessionCookieVerifier) {
        this.sessionCookieVerifier = sessionCookieVerifier;
    }

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

        Cookie sessionCookie = findSessionCookie(httpRequest.getCookies());
        if (sessionCookie != null) {
            Optional<String> userId = sessionCookieVerifier.verifyAndExtractUserId(sessionCookie.getValue());
            if (userId.isPresent()) {
                PreAuthenticatedAuthenticationToken authToken = new PreAuthenticatedAuthenticationToken(
                        userId.get(),
                        null,
                        Collections.emptyList()
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
}
