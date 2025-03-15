package com.lovettj.surfspotsapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.service.PasswordResetService;
import com.lovettj.surfspotsapi.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User userRequest) {
        try {
            userService.registerUser(userRequest);
            return ResponseEntity.ok("Account created successfully!");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserProfile> loginUser(@RequestBody User userRequest) {
        try {
            User authenticatedUser = userService.loginUser(userRequest.getEmail(), userRequest.getPassword());
            return ResponseEntity.ok(new UserProfile(authenticatedUser));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(401).body(null);
        }
    }

    @PutMapping("/update-password")
    public ResponseEntity<String> updatePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            userService.updatePassword(changePasswordRequest);
            return ResponseEntity.ok("Password changed successfully!");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to change password");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email, @RequestHeader(name = "Origin") String origin) {
        try {
            passwordResetService.createPasswordResetToken(email, origin);
            return ResponseEntity.ok("Password reset link sent if email exists.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to send reset link");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        try {
            passwordResetService.resetPassword(token, newPassword);
            return ResponseEntity.ok("Password successfully reset.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to change password");
        }
    }
}
