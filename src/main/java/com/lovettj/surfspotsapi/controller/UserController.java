package com.lovettj.surfspotsapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.requests.SettingsRequest;
import com.lovettj.surfspotsapi.requests.UserRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * Returns the authenticated user's full profile. The session cookie only carries
     * minimum identity claims (id / email / name); pages that need richer data
     * should hit this endpoint so profile fields never need to ride along in
     * the client-side session cookie.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfile>> getCurrentUser() {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            return userService.getUserProfile(currentUserId)
                    .map(profile -> ResponseEntity.ok(ApiResponse.success(profile, "Profile loaded")))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error(ApiErrors.USER_NOT_FOUND, HttpStatus.NOT_FOUND.value())));
        } catch (ResponseStatusException e) {
            int code = e.getStatusCode().value();
            return ResponseEntity.status(code)
                    .body(ApiResponse.error(e.getReason() != null ? e.getReason()
                            : ApiErrors.formatErrorMessage("load", "profile"), code));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("load", "profile"),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/update/profile")
    public ResponseEntity<ApiResponse<String>> updateUser(@Valid @RequestBody UserRequest user) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            userService.updateUserProfile(currentUserId, user);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully!"));
        } catch (ResponseStatusException e) {
            int code = e.getStatusCode().value();
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason() != null ? e.getReason() : ApiErrors.formatErrorMessage("update", "profile"), code));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("update", "profile"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/update-password")
    public ResponseEntity<ApiResponse<String>> updatePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            changePasswordRequest.setUserId(currentUserId);
            userService.updatePassword(changePasswordRequest);
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully!"));
        } catch (ResponseStatusException e) {
            int code = e.getStatusCode().value();
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason() != null ? e.getReason() : ApiErrors.formatErrorMessage("change", "password"), code));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("change", "password"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<String>> updateSettings(@RequestBody SettingsRequest settingsRequest) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            settingsRequest.setUserId(currentUserId);
            userService.updateSettings(settingsRequest);
            return ResponseEntity.ok(ApiResponse.success("Settings updated successfully!"));
        } catch (ResponseStatusException e) {
            int code = e.getStatusCode().value();
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason() != null ? e.getReason() : ApiErrors.formatErrorMessage("update", "settings"), code));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("update", "settings"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Account delete lives under {@code /account/...} so it never collides with
     * {@code GET /api/user/me}. A regex on {@code /{userId}} is fragile: repetition
     * braces in the regex can close the Spring path-variable block early.
     */
    @DeleteMapping("/account/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteAccount(@PathVariable String userId) {
        try {
            userService.deleteAccount(userId);
            return ResponseEntity.ok(ApiResponse.success("Account deleted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason() != null ? e.getReason() : ApiErrors.formatErrorMessage("delete", "account"), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("delete", "account"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
