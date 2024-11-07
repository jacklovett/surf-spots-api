package com.lovettj.surfspotsapi.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.responses.ApiResponse;
import com.lovettj.surfspotsapi.exceptions.AuthException;
import com.lovettj.surfspotsapi.exceptions.SurfSpotsException;
import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.service.UserService;

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
  public ResponseEntity<ApiResponse> updateUser(@RequestBody User user) {
    try {
      userService.updateUserProfile(user);
      return ResponseEntity.ok(new ApiResponse("Profile updated successfully!"));
    } catch (SurfSpotsException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new ApiResponse("Unable to update profile"));
    }
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse> registerUser(@RequestBody User userRequest) {
    try {
      userService.registerUser(userRequest);
      return ResponseEntity.ok(new ApiResponse("Account created successfully!"));
    } catch (AuthException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(new ApiResponse(e.getMessage()));
    }
  }

  @PostMapping("/login")
  public ResponseEntity<UserProfile> loginUser(@RequestBody User userRequest) {
    try {
      User authenticatedUser = userService.loginUser(userRequest.getEmail(), userRequest.getPassword());
      return ResponseEntity.ok(new UserProfile(authenticatedUser));
    } catch (AuthException e) {
      // Return 401 Unauthorized if the user is not found or password is invalid
      return ResponseEntity.status(401).body(null);
    }
  }
}
