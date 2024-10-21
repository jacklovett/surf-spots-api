package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.UserSurfSpotRequest;
import com.lovettj.surfspotsapi.service.WatchListService;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchListController {
  private final WatchListService watchListService;

  public WatchListController(WatchListService watchListService) {
    this.watchListService = watchListService;
  }

  @GetMapping("/{userId}")
  public ResponseEntity<List<SurfSpot>> getUsersWatchList(@PathVariable Long userId) {
    return ResponseEntity.ok(watchListService.getUsersWatchList(userId));
  }

  @PostMapping
  public ResponseEntity<String> addWatchListSurfSpot(@RequestBody UserSurfSpotRequest request) {
    Long userId = request.getUserId();
    Long spotId = request.getSurfSpotId();
    watchListService.addSurfSpotToWatchList(userId, spotId);
    return ResponseEntity.ok("Surf spot added to user’s watch list.");
  }

  @DeleteMapping("/{userId}/remove/{spotId}")
  public ResponseEntity<String> removeWatchListSurfSpot(@PathVariable Long userId, @PathVariable Long spotId) {
    watchListService.removeSurfSpotFromWishList(userId, spotId);
    return ResponseEntity.ok("Surf spot removed from user’s watch list.");
  }
}
