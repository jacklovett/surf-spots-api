package com.lovettj.surfspotsapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.requests.SettingsRequest;
import com.lovettj.surfspotsapi.requests.UserRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/update/profile")
    public ResponseEntity<ApiResponse<String>> updateUser(@Valid @RequestBody UserRequest user) {
        try {
            userService.updateUserProfile(user);
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

    @DeleteMapping("/{userId}")
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
