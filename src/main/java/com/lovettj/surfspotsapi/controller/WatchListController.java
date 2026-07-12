package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.dto.WatchListDTO;
import com.lovettj.surfspotsapi.requests.UserSurfSpotRequest;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.WatchListService;

@RestController
@RequestMapping("/api/watch")
public class WatchListController {
  private final WatchListService watchListService;
  private final AuthenticatedUserResolver authenticatedUserResolver;

  public WatchListController(
      WatchListService watchListService,
      AuthenticatedUserResolver authenticatedUserResolver) {
    this.watchListService = watchListService;
    this.authenticatedUserResolver = authenticatedUserResolver;
  }

  @GetMapping
  @ApiFailureMessage(action = "load", target = "watch list")
  public ResponseEntity<WatchListDTO> getUsersWatchList() {
    String currentUserId = authenticatedUserResolver.requireCurrentUserId();
    return ResponseEntity.ok(watchListService.getUsersWatchList(currentUserId));
  }

  @PostMapping
  @ApiFailureMessage(action = "add", target = "surf spot to watch list")
  public ResponseEntity<ApiResponse<String>> addSurfSpotToWatchList(@RequestBody UserSurfSpotRequest request) {
    request.setUserId(authenticatedUserResolver.requireCurrentUserId());
    watchListService.addSurfSpotToWatchList(request.getUserId(), request.getSurfSpotId());
    return ResponseEntity.ok(ApiResponse.success("Surf spot added to user's watch list."));
  }

  @DeleteMapping("/remove/{spotId}")
  @ApiFailureMessage(action = "remove", target = "surf spot from watch list")
  public ResponseEntity<ApiResponse<String>> removeWatchListSurfSpot(@PathVariable Long spotId) {
    String currentUserId = authenticatedUserResolver.requireCurrentUserId();
    watchListService.removeSurfSpotFromWishList(currentUserId, spotId);
    return ResponseEntity.ok(ApiResponse.success("Surf spot removed from user's watch list."));
  }
}
