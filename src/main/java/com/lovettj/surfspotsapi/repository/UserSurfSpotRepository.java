package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.UserSurfSpot;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSurfSpotRepository extends JpaRepository<UserSurfSpot, String> {
    Optional<UserSurfSpot> findByUserIdAndSurfSpotId(String userId, Long spotId);
    List<UserSurfSpot> findByUserId(String userId);
}
