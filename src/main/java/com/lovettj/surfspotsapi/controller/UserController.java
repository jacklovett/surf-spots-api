package com.lovettj.surfspotsapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.service.UserService;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/{userId}/profile")
  public ResponseEntity<UserProfile> getUserProfile(@PathVariable Long userId) {
    Optional<UserProfile> userProfile = userService.getUserProfile(userId);
    return userProfile.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PutMapping("/{userId}/profile")
  public ResponseEntity<UserProfile> updateUserProfile(@PathVariable Long userId,
      @RequestBody UserProfile updatedProfile) {
    UserProfile userProfile = userService.updateUserProfile(userId, updatedProfile);
    return ResponseEntity.ok(userProfile);
  }

  @PostMapping("/auth")
  public void auth(RequestBody request) {
    userService.auth();
  }

  @PostMapping("/logout")
  public void logout(RequestBody request) {
    userService.logout();
  }
}