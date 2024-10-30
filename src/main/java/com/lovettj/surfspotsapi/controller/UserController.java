package com.lovettj.surfspotsapi.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.exceptions.AuthException;
import com.lovettj.responses.ApiResponse;
import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.service.UserService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  // Existing endpoint to save or find the user
  @PostMapping("/profile")
  public ResponseEntity<UserProfile> saveUserProfile(@RequestBody AuthRequest userRequest) {
    // Save or find the user in the database
    User user = userService.findOrCreateUser(userRequest);
    return ResponseEntity.ok(new UserProfile(user)); // Return UserProfile DTO
  }

  // Existing registration endpoint
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
      return ResponseEntity.ok(new UserProfile(authenticatedUser)); // Return UserProfile DTO
    } catch (AuthException e) {
      // Return 401 Unauthorized if the user is not found or password is invalid
      return ResponseEntity.status(401).body(null);
    }
  }
}
