package com.lovettj.surfspotsapi.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;

  public Optional<UserProfile> getUserProfile(Long userId) {
    return userRepository.findById(userId)
        .map(UserProfile::new);
  }

  public UserProfile updateUserProfile(Long userId, UserProfile updatedProfile) {
    return userRepository.findById(userId)
        .map(user -> {
          user.setUsername(updatedProfile.getUsername());
          user.setName(updatedProfile.getName());
          user.setEmail(updatedProfile.getEmail());
          user.setCountry(updatedProfile.getCountry());
          user.setRegion(updatedProfile.getRegion());

          userRepository.save(user);
          return new UserProfile(user);
        })
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

  public void auth() {

  }

  public void logout() {

  }
}
