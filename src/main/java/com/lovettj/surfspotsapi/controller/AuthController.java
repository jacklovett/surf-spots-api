package com.lovettj.surfspotsapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.lovettj.surfspotsapi.config.AppProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.email.EmailLayoutVariables;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.ClientIpExtractor;
import com.lovettj.surfspotsapi.security.RateLimiter;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.service.EmailVerificationService;
import com.lovettj.surfspotsapi.service.PasswordResetService;
import com.lovettj.surfspotsapi.service.UserService;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.EmailRequest;
import com.lovettj.surfspotsapi.requests.ResetPasswordRequest;
import com.lovettj.surfspotsapi.requests.VerifyEmailRequest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final UserService userService;
    private final RateLimiter rateLimiter;
    private final AppProperties appProperties;

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmailFromEmailLink(
            @RequestParam(name = "token", required = false) String token,
            HttpServletRequest httpRequest) {
        String clientIp = ClientIpExtractor.extract(httpRequest);
        String frontendBase = EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getUrl());
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendBase + "/auth?verifyError=missing"))
                    .build();
        }
        
        try {
            rateLimiter.checkRateLimit(RateLimiter.Bucket.VERIFY_EMAIL, clientIp);
            emailVerificationService.verifyEmailWithToken(token.trim());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendBase + "/auth?verified=true"))
                    .build();
        } catch (ResponseStatusException e) {
            int status = e.getStatusCode().value();
            if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(frontendBase + "/auth?verifyError=rate_limit"))
                        .build();
            }
            if (status == HttpStatus.BAD_REQUEST.value()) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(frontendBase + "/auth?verifyError=invalid"))
                        .build();
            }
            log.warn("verify-email GET unexpected ResponseStatusException status={}", status, e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendBase + "/auth?verifyError=server"))
                    .build();
        } catch (Exception e) {
            log.error("verify-email GET failed", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendBase + "/auth?verifyError=server"))
                    .build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfile>> registerUser(
            @RequestBody AuthRequest authRequest,
            HttpServletRequest httpRequest) {
        String clientIp = ClientIpExtractor.extract(httpRequest);
        try {
            rateLimiter.checkRateLimit(RateLimiter.Bucket.REGISTER, clientIp);
            User user = userService.registerUser(authRequest);
            URI location = CreatedResourceLocations.fromApiPath("/api/user/{userId}", null, user.getId());
            UserProfile profile = new UserProfile(user);
            String message =
                    profile.isEmailVerified()
                            ? "Account created successfully"
                            : "Account created successfully. We sent a verification link to your email.";
            return ResponseEntity.created(location)
                    .body(ApiResponse.success(profile, message, HttpStatus.CREATED.value()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason() != null ? e.getReason() : "Request failed.", e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("create", "account"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserProfile>> loginUser(
            @RequestBody AuthRequest authRequest,
            HttpServletRequest httpRequest) {
        String clientIp = ClientIpExtractor.extract(httpRequest);
        String email = authRequest.getEmail();
        try {
            rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, clientIp);
            rateLimiter.checkRateLimit(RateLimiter.Bucket.LOGIN, email);
            User authenticatedUser = userService.loginUser(email, authRequest.getPassword());
            rateLimiter.reset(RateLimiter.Bucket.LOGIN, clientIp);
            rateLimiter.reset(RateLimiter.Bucket.LOGIN, email);
            return ResponseEntity.ok(ApiResponse.success(new UserProfile(authenticatedUser), "Login successful"));
        } catch (ResponseStatusException e) {
            int status = e.getStatusCode().value();
            String safeReason = e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                    || e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                            ? ApiErrors.INVALID_CREDENTIALS
                            : (e.getReason() != null ? e.getReason() : "Request failed.");
            int safeStatus = e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                    ? HttpStatus.UNAUTHORIZED.value()
                    : e.getStatusCode().value();
            return ResponseEntity.status(safeStatus)
                    .body(ApiResponse.error(safeReason, safeStatus));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("sign in", null), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = ClientIpExtractor.extract(httpRequest);
        try {
            rateLimiter.checkRateLimit(RateLimiter.Bucket.VERIFY_EMAIL, clientIp);
            emailVerificationService.verifyEmailWithToken(request.getToken());
            return ResponseEntity.ok(ApiResponse.success(null, "Email verified. Thank you."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(
                            e.getReason() != null ? e.getReason() : ApiErrors.VERIFY_EMAIL_TOKEN_INVALID_OR_EXPIRED,
                            e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("verify", "email"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerification(
            @RequestBody EmailRequest request,
            @RequestHeader(name = "Origin", required = false) String origin,
            @RequestHeader(name = "Referer", required = false) String referer,
            HttpServletRequest httpRequest) {
        String clientIp = ClientIpExtractor.extract(httpRequest);
        try {
            emailVerificationService.resendVerificationEmail(request.getEmail(), origin, referer, clientIp);
        } catch (ResponseStatusException e) {
            int status = e.getStatusCode().value();
            if (status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.TOO_MANY_REQUESTS.value()) {
                return ResponseEntity.status(status)
                        .body(ApiResponse.error(e.getReason() != null ? e.getReason() : "Request failed.", status));
            }
        } catch (Exception e) {
            // Real outage or bug: do not claim the email was sent. Use a single generic message (no internals).
            log.error("Resend verification email failed unexpectedly", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ApiErrors.SOMETHING_WENT_WRONG,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
        return ResponseEntity.ok(ApiResponse.success(null, ApiErrors.RESEND_VERIFICATION_ACCEPTED));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody EmailRequest request,
            @RequestHeader(name = "Origin", required = false) String origin,
            @RequestHeader(name = "Referer", required = false) String referer,
            HttpServletRequest httpRequest) {
        String clientIp = ClientIpExtractor.extract(httpRequest);
        try {
            passwordResetService.createPasswordResetToken(request.getEmail(), origin, referer, clientIp);
        } catch (ResponseStatusException e) {
            // Preserve legitimate 400 (bad origin) and 429 (rate limit) responses, but
            // never leak other codes that could indicate account existence.
            int status = e.getStatusCode().value();
            if (status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.TOO_MANY_REQUESTS.value()) {
                return ResponseEntity.status(status)
                        .body(ApiResponse.error(e.getReason() != null ? e.getReason() : "Request failed.", status));
            }
        } catch (Exception e) {
            // Real outage or bug: do not claim the reset email was sent. Use a single generic message (no internals).
            log.error("Forgot-password flow failed unexpectedly", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ApiErrors.SOMETHING_WENT_WRONG,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
        return ResponseEntity.ok(
                ApiResponse.success(null, ApiErrors.FORGOT_PASSWORD_ACCEPTED));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Password successfully reset."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason() != null ? e.getReason() : ApiErrors.formatErrorMessage("change", "password"), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("change", "password"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
