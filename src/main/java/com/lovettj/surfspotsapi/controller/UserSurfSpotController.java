package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.UserSurfSpotRequest;
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
  public ResponseEntity<List<SurfSpot>> getUserSurfSpots(@PathVariable Long userId) {
    return ResponseEntity.ok(userSurfSpotService.getUserSurfSpots(userId));
  }

  @PostMapping
  public ResponseEntity<String> addUserSurfSpot(@RequestBody UserSurfSpotRequest request) {
    Long userId = request.getUserId();
    Long spotId = request.getSurfSpotId();
    userSurfSpotService.addUserSurfSpot(userId, spotId);
    return ResponseEntity.ok("Surf spot added to user’s list.");
  }

  @DeleteMapping("/{userId}/remove/{spotId}")
  public ResponseEntity<String> removeUserSurfSpot(@PathVariable Long userId, @PathVariable Long spotId) {
    userSurfSpotService.removeUserSurfSpot(userId, spotId);
    return ResponseEntity.ok("Surf spot removed from user’s list.");
  }

  @PostMapping("/{userId}/toggle-favourite/{spotId}")
  public ResponseEntity<String> toggleFavourite(@PathVariable Long userId, @PathVariable Long spotId) {
    userSurfSpotService.toggleIsFavourite(userId, spotId);
    return ResponseEntity.ok("Surf spot favourite status toggled.");
  }
}
