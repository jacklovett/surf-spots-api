package com.lovettj.surfspotsapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.requests.SettingsRequest;
import com.lovettj.surfspotsapi.requests.UserLocationRequest;
import com.lovettj.surfspotsapi.requests.UserRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.NearbyTravelNotificationService;
import com.lovettj.surfspotsapi.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final NearbyTravelNotificationService nearbyTravelNotificationService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * Returns the authenticated user's full profile. The session cookie only carries
     * minimum identity claims (id / email / name); pages that need richer data
     * should hit this endpoint so profile fields never need to ride along in
     * the client-side session cookie.
     */
    @GetMapping("/me")
    @ApiFailureMessage(action = "load", target = "profile")
    public ResponseEntity<ApiResponse<UserProfile>> getCurrentUser() {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return userService.getUserProfile(currentUserId)
                .map(profile -> ResponseEntity.ok(ApiResponse.success(profile, "Profile loaded")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(ApiErrors.USER_NOT_FOUND, HttpStatus.NOT_FOUND.value())));
    }

    @PutMapping("/update/profile")
    @ApiFailureMessage(action = "update", target = "profile")
    public ResponseEntity<ApiResponse<String>> updateUser(@Valid @RequestBody UserRequest user) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        userService.updateUserProfile(currentUserId, user);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully!"));
    }

    @PutMapping("/update-password")
    @ApiFailureMessage(action = "change", target = "password")
    public ResponseEntity<ApiResponse<String>> updatePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        changePasswordRequest.setUserId(currentUserId);
        userService.updatePassword(changePasswordRequest);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully!"));
    }

    @PutMapping("/settings")
    @ApiFailureMessage(action = "update", target = "settings")
    public ResponseEntity<ApiResponse<String>> updateSettings(@RequestBody SettingsRequest settingsRequest) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        settingsRequest.setUserId(currentUserId);
        userService.updateSettings(settingsRequest);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully!"));
    }

    /**
     * Reports the signed-in user's current browser/device location for nearby-travel alerts.
     * First report only stores a baseline; later reports may email if the jump is large
     * and {@code nearbySurfSpotsEmails} is enabled.
     */
    @PostMapping("/location")
    @ApiFailureMessage(action = "update", target = "location")
    public ResponseEntity<ApiResponse<String>> reportLocation(
            @Valid @RequestBody UserLocationRequest locationRequest) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        nearbyTravelNotificationService.reportLocation(
                currentUserId, locationRequest.getLatitude(), locationRequest.getLongitude());
        return ResponseEntity.ok(ApiResponse.success("Location updated"));
    }

    /**
     * Account delete lives under {@code /account/...} so it never collides with
     * {@code GET /api/user/me}. Path variable is {@code accountUserId} (not {@code userId}) so
     * {@link AuthenticatedUserInterceptor} does not treat the segment as a generic {@code userId}
     * match check; the controller still enforces that only the signed-in user may delete their account.
     */
    @DeleteMapping("/account/{accountUserId}")
    @ApiFailureMessage(action = "delete", target = "account")
    public ResponseEntity<ApiResponse<String>> deleteAccount(@PathVariable String accountUserId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        if (!currentUserId.equals(accountUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrors.ACCOUNT_DELETE_NOT_PERMITTED);
        }
        userService.deleteAccount(accountUserId);
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully"));
    }
}
