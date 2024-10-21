package com.lovettj.surfspotsapi.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;
import com.lovettj.surfspotsapi.repository.WatchListRepository;

@Service
public class WatchListService {
  private final WatchListRepository watchListRepository;

  public WatchListService(WatchListRepository watchListRepository) {
    this.watchListRepository = watchListRepository;
  }

  /**
   * Add surf spot to the users watchList
   * 
   * @param userId
   * @param spotId
   */
  public void addSurfSpotToWatchList(Long userId, Long spotId) {
    // Check if the spot is already in the user's list
    Optional<WatchListSurfSpot> existingEntry = watchListRepository.findByUserIdAndSurfSpotId(userId, spotId);

    if (existingEntry.isEmpty()) {
      // If not found, create a new entry
      WatchListSurfSpot newEntry = WatchListSurfSpot.builder()
          .user(User.builder().id(userId).build()) // Assuming User object is fetched correctly
          .surfSpot(SurfSpot.builder().id(spotId).build()) // Assuming SurfSpot object is fetched correctly
          .build();

      watchListRepository.save(newEntry);
    }
  }

  /**
   * Find and remove the surf spot from the user's watchList
   * 
   * @param userId
   * @param spotId
   */
  public void removeSurfSpotFromWishList(Long userId, Long spotId) {
    Optional<WatchListSurfSpot> existingEntry = watchListRepository.findByUserIdAndSurfSpotId(userId, spotId);
    existingEntry.ifPresent(watchListRepository::delete);
  }

  /**
   * Get users surf spot watchList
   * 
   * @param userId
   * @return surfSpots
   */
  public List<SurfSpot> getUsersWatchList(Long userId) {
    List<WatchListSurfSpot> watchList = watchListRepository.findByUserId(userId);
    return watchList.stream()
        .map(WatchListSurfSpot::getSurfSpot)
        .collect(Collectors.toList());
  }
}
