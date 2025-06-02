package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.dto.WatchListDTO;
import com.lovettj.surfspotsapi.requests.UserSurfSpotRequest;
import com.lovettj.surfspotsapi.service.WatchListService;

@RestController
@RequestMapping("/api/watch")
public class WatchListController {
  private final WatchListService watchListService;

  public WatchListController(WatchListService watchListService) {
    this.watchListService = watchListService;
  }

  @GetMapping("/{userId}")
  public ResponseEntity<WatchListDTO> getUsersWatchList(@PathVariable String userId) {
    return ResponseEntity.ok(watchListService.getUsersWatchList(userId));
  }

  @PostMapping
  public ResponseEntity<String> addSurfSpotToWatchList(@RequestBody UserSurfSpotRequest request) {
    watchListService.addSurfSpotToWatchList(request.getUserId(), request.getSurfSpotId());
    return ResponseEntity.ok("Surf spot added to user’s watch list.");
  }

  @DeleteMapping("/{userId}/remove/{spotId}")
  public ResponseEntity<String> removeWatchListSurfSpot(@PathVariable String userId, @PathVariable Long spotId) {
    watchListService.removeSurfSpotFromWishList(userId, spotId);
    return ResponseEntity.ok("Surf spot removed from user’s watch list.");
  }
}
