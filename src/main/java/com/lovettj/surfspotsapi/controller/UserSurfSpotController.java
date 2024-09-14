package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.entity.UserSurfSpot;
import com.lovettj.surfspotsapi.service.UserSurfSpotService;

import java.util.List;

@RestController
@RequestMapping("/api/user-spots")
public class UserSurfSpotController {
  private final UserSurfSpotService userSurfSpotService;

  public UserSurfSpotController(UserSurfSpotService userSurfSpotService) {
    this.userSurfSpotService = userSurfSpotService;
  }

  @GetMapping("/{userId}")
  public ResponseEntity<List<UserSurfSpot>> getUserSurfSpots(@PathVariable Long userId) {
    return ResponseEntity.ok(userSurfSpotService.getUserSurfSpots(userId));
  }

  @PostMapping("/{userId}/add/{spotId}")
  public ResponseEntity<String> addUserSurfSpot(@PathVariable Long userId, @PathVariable Long spotId) {
    userSurfSpotService.addUserSurfSpot(userId, spotId);
    return ResponseEntity.ok("Surf spot added to user’s list.");
  }

  @DeleteMapping("/{userId}/remove/{spotId}")
  public ResponseEntity<String> removeUserSurfSpot(@PathVariable Long userId, @PathVariable Long spotId) {
    userSurfSpotService.removeUserSurfSpot(userId, spotId);
    return ResponseEntity.ok("Surf spot removed from user’s list.");
  }

  @PostMapping("/{userId}/toggle-favorite/{spotId}")
  public ResponseEntity<String> toggleFavorite(@PathVariable Long userId, @PathVariable Long spotId) {
    userSurfSpotService.toggleIsFavorite(userId, spotId);
    return ResponseEntity.ok("Surf spot favorite status toggled.");
  }
}
