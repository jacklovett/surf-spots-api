package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.entity.WishlistSurfSpot;
import com.lovettj.surfspotsapi.service.WishlistSurfSpotService;

import java.util.List;

@RestController
@RequestMapping("/api/spots-wishlist")
public class WishlistSurfSpotController {
  private final WishlistSurfSpotService wishlistSurfSpotService;

  public WishlistSurfSpotController(WishlistSurfSpotService wishlistSurfSpotService) {
    this.wishlistSurfSpotService = wishlistSurfSpotService;
  }

  @GetMapping("/{userId}")
  public ResponseEntity<List<WishlistSurfSpot>> getUsersWishlist(@PathVariable Long userId) {
    return ResponseEntity.ok(wishlistSurfSpotService.getUsersWishlist(userId));
  }

  @PostMapping("/{userId}/add/{spotId}")
  public ResponseEntity<String> addWishlistSurfSpot(@PathVariable Long userId, @PathVariable Long spotId) {
    wishlistSurfSpotService.addSurfSpotToWishlist(userId, spotId);
    return ResponseEntity.ok("Surf spot added to user’s list.");
  }

  @DeleteMapping("/{userId}/remove/{spotId}")
  public ResponseEntity<String> removeWishlistSurfSpot(@PathVariable Long userId, @PathVariable Long spotId) {
    wishlistSurfSpotService.removeSurfSpotFromWishList(userId, spotId);
    return ResponseEntity.ok("Surf spot removed from user’s list.");
  }
}
