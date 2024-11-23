package com.lovettj.surfspotsapi.service;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.UserSurfSpot;
import com.lovettj.surfspotsapi.repository.UserSurfSpotRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserSurfSpotService {
  private final UserSurfSpotRepository userSurfSpotRepository;

  public UserSurfSpotService(UserSurfSpotRepository userSurfSpotRepository) {
    this.userSurfSpotRepository = userSurfSpotRepository;
  }

  public List<SurfSpot> getUserSurfSpots(Long userId) {
    List<UserSurfSpot> userSurfSpots = userSurfSpotRepository.findByUserId(userId);
    return userSurfSpots.stream()
        .map(UserSurfSpot::getSurfSpot)
        .collect(Collectors.toList());
  }

  public void addUserSurfSpot(Long userId, Long spotId) {
    // Check if the spot is already in the user's list
    Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);

    if (existingEntry.isEmpty()) {
      // If not found, create a new entry
      UserSurfSpot newEntry = UserSurfSpot.builder()
          .user(User.builder().id(userId).build()) // Assuming User object is fetched correctly
          .surfSpot(SurfSpot.builder().id(spotId).build()) // Assuming SurfSpot object is fetched correctly
          .isFavourite(false)
          .build();
      userSurfSpotRepository.save(newEntry);
    }
  }

  public boolean isUserSurfedSpot(Long userId, Long spotId) {
    Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);
    return existingEntry.isPresent();
  }

  public void removeUserSurfSpot(Long userId, Long spotId) {
    Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);
    existingEntry.ifPresent(userSurfSpotRepository::delete);
  }

  public void toggleIsFavourite(Long userId, Long spotId) {
    Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);

    if (existingEntry.isPresent()) {
      UserSurfSpot userSurfSpot = existingEntry.get();
      userSurfSpot.setFavourite(!userSurfSpot.isFavourite());
      userSurfSpotRepository.save(userSurfSpot);
    }
  }
}
