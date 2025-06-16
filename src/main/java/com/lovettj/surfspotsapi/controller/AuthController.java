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
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.service.PasswordResetService;
import com.lovettj.surfspotsapi.service.UserService;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.EmailRequest;
import com.lovettj.surfspotsapi.requests.ResetPasswordRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfile>> registerUser(@RequestBody AuthRequest authRequest) {
        try {
            User user = userService.registerUser(authRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new UserProfile(user), "Account created successfully", HttpStatus.CREATED.value()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserProfile>> loginUser(@RequestBody AuthRequest authRequest) {
        try {
            User authenticatedUser = userService.loginUser(authRequest.getEmail(), authRequest.getPassword());
            return ResponseEntity.ok(ApiResponse.success(new UserProfile(authenticatedUser), "Login successful"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody EmailRequest request,
            @RequestHeader(name = "Origin") String origin) {
        try {
            passwordResetService.createPasswordResetToken(request.getEmail(), origin);
            return ResponseEntity.ok(ApiResponse.success("Password reset link sent if email exists."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to send reset link", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Password successfully reset."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to change password", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
