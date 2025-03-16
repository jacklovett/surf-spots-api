package com.lovettj.surfspotsapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/profile")
    public ResponseEntity<UserProfile> saveUserProfile(@RequestBody AuthRequest userRequest) {
        // Save or find the user in the database
        User user = userService.findOrCreateUser(userRequest);
        return ResponseEntity.ok(new UserProfile(user));
    }

    @PutMapping("/update/profile")
    public ResponseEntity<String> updateUser(@RequestBody User user) {
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
}
