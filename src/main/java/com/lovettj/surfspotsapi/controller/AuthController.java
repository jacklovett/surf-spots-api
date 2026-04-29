package com.lovettj.surfspotsapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.ClientIpExtractor;
import com.lovettj.surfspotsapi.security.RateLimiter;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.service.PasswordResetService;
import com.lovettj.surfspotsapi.service.UserService;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.EmailRequest;
import com.lovettj.surfspotsapi.requests.ResetPasswordRequest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final RateLimiter rateLimiter;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfile>> registerUser(
            @RequestBody AuthRequest authRequest,
            HttpServletRequest httpRequest) {
        String clientIp = ClientIpExtractor.extract(httpRequest);
        try {
            rateLimiter.checkRateLimit(RateLimiter.Bucket.REGISTER, clientIp);
            User user = userService.registerUser(authRequest);
            URI location = CreatedResourceLocations.fromApiPath("/api/user/{userId}", null, user.getId());
            return ResponseEntity.created(location)
                .body(ApiResponse.success(new UserProfile(user), "Account created successfully", HttpStatus.CREATED.value()));
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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody EmailRequest request,
            @RequestHeader(name = "Origin") String origin,
            HttpServletRequest httpRequest) {
        String clientIp = ClientIpExtractor.extract(httpRequest);
        try {
            passwordResetService.createPasswordResetToken(request.getEmail(), origin, clientIp);
        } catch (ResponseStatusException e) {
            // Preserve legitimate 400 (bad origin) and 429 (rate limit) responses, but
            // never leak other codes that could indicate account existence.
            int status = e.getStatusCode().value();
            if (status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.TOO_MANY_REQUESTS.value()) {
                return ResponseEntity.status(status)
                        .body(ApiResponse.error(e.getReason() != null ? e.getReason() : "Request failed.", status));
            }
        } catch (Exception e) {
            // Swallow internal errors and still return the generic success shape —
            // otherwise an attacker could probe by timing or by status-code drift.
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
