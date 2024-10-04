package com.lovettj.surfspotsapi.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.WishlistSurfSpot;
import com.lovettj.surfspotsapi.repository.WishlistSurfSpotRepository;

@Service
public class WishlistSurfSpotService {
  private final WishlistSurfSpotRepository wishlistSurfSpotRepository;

  public WishlistSurfSpotService(WishlistSurfSpotRepository wishlistSurfSpotRepository) {
    this.wishlistSurfSpotRepository = wishlistSurfSpotRepository;
  }

  /**
   * Add surf spot to the users wishlist
   * 
   * @param userId
   * @param spotId
   */
  public void addSurfSpotToWishlist(Long userId, Long spotId) {
    // Check if the spot is already in the user's list
    Optional<WishlistSurfSpot> existingEntry = wishlistSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);

    if (existingEntry.isEmpty()) {
      // If not found, create a new entry
      WishlistSurfSpot newEntry = WishlistSurfSpot.builder()
          .user(User.builder().id(userId).build()) // Assuming User object is fetched correctly
          .surfSpot(SurfSpot.builder().id(spotId).build()) // Assuming SurfSpot object is fetched correctly
          .build();

      wishlistSurfSpotRepository.save(newEntry);
    }
  }

  /**
   * Find and remove the surf spot from the user's wishlist
   * 
   * @param userId
   * @param spotId
   */
  public void removeSurfSpotFromWishList(Long userId, Long spotId) {
    Optional<WishlistSurfSpot> existingEntry = wishlistSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);
    existingEntry.ifPresent(wishlistSurfSpotRepository::delete);
  }

  /**
   * Get users surf spot wishlist
   * 
   * @param userId
   * @return surfSpots
   */
  public List<SurfSpot> getUsersWishlist(Long userId) {
    List<WishlistSurfSpot> wishlist = wishlistSurfSpotRepository.findByUserId(userId);
    return wishlist.stream()
        .map(WishlistSurfSpot::getSurfSpot)
        .collect(Collectors.toList());
  }
}
