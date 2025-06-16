package com.lovettj.surfspotsapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.requests.SettingsRequest;
import com.lovettj.surfspotsapi.requests.UserRequest;
import com.lovettj.surfspotsapi.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/update/profile")
    public ResponseEntity<String> updateUser(@RequestBody UserRequest user) {
        try {
            userService.updateUserProfile(user);
            return ResponseEntity.ok("Profile updated successfully!");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to update profile");
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

    @PutMapping("/settings")
    public ResponseEntity<String> updateSettings(@RequestBody SettingsRequest settingsRequest) {
        try {
            userService.updateSettings(settingsRequest);
            return ResponseEntity.ok("Settings updated successfully!");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to update settings");
        }
    }
}
