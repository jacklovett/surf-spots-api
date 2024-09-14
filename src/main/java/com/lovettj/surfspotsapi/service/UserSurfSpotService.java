package com.lovettj.surfspotsapi.service;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.UserSurfSpot;
import com.lovettj.surfspotsapi.repository.UserSurfSpotRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserSurfSpotService {
  private final UserSurfSpotRepository userSurfSpotRepository;

  public UserSurfSpotService(UserSurfSpotRepository userSurfSpotRepository) {
    this.userSurfSpotRepository = userSurfSpotRepository;
  }

  public void addUserSurfSpot(Long userId, Long spotId) {
    // Check if the spot is already in the user's list
    Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);

    if (existingEntry.isEmpty()) {
      // If not found, create a new entry
      UserSurfSpot newEntry = UserSurfSpot.builder()
          .user(User.builder().id(userId).build()) // Assuming User object is fetched correctly
          .surfSpot(SurfSpot.builder().id(spotId).build()) // Assuming SurfSpot object is fetched correctly
          .isFavourite(false) // Default to not a favorite
          .build();
      userSurfSpotRepository.save(newEntry);
    }
  }

  public void removeUserSurfSpot(Long userId, Long spotId) {
    // Find and remove the surf spot from the user's list
    Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);

    existingEntry.ifPresent(userSurfSpotRepository::delete);
  }

  public void toggleIsFavorite(Long userId, Long spotId) {
    Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);

    if (existingEntry.isPresent()) {
      UserSurfSpot userSurfSpot = existingEntry.get();
      userSurfSpot.setFavourite(!userSurfSpot.isFavourite());
      userSurfSpotRepository.save(userSurfSpot);
    }
  }

  public List<UserSurfSpot> getUserSurfSpots(Long userId) {
    return userSurfSpotRepository.findByUserId(userId);
  }
}
