package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.UserSurfSpotRequest;
import com.lovettj.surfspotsapi.service.WishlistSurfSpotService;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistSurfSpotController {
  private final WishlistSurfSpotService wishlistSurfSpotService;

  public WishlistSurfSpotController(WishlistSurfSpotService wishlistSurfSpotService) {
    this.wishlistSurfSpotService = wishlistSurfSpotService;
  }

  @GetMapping("/{userId}")
  public ResponseEntity<List<SurfSpot>> getUsersWishlist(@PathVariable Long userId) {
    return ResponseEntity.ok(wishlistSurfSpotService.getUsersWishlist(userId));
  }

  @PostMapping
  public ResponseEntity<String> addWishlistSurfSpot(@RequestBody UserSurfSpotRequest request) {
    Long userId = request.getUserId();
    Long spotId = request.getSurfSpotId();
    wishlistSurfSpotService.addSurfSpotToWishlist(userId, spotId);
    return ResponseEntity.ok("Surf spot added to user’s wishlist.");
  }

  @DeleteMapping("/{userId}/remove/{spotId}")
  public ResponseEntity<String> removeWishlistSurfSpot(@PathVariable Long userId, @PathVariable Long spotId) {
    wishlistSurfSpotService.removeSurfSpotFromWishList(userId, spotId);
    return ResponseEntity.ok("Surf spot removed from user’s list.");
  }
}
