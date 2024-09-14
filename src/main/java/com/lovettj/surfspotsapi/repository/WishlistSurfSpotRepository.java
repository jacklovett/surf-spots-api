package com.lovettj.surfspotsapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lovettj.surfspotsapi.entity.WishlistSurfSpot;

public interface WishlistSurfSpotRepository extends JpaRepository<WishlistSurfSpot, Long> {
  List<WishlistSurfSpot> findByUserId(Long userId);

  Optional<WishlistSurfSpot> findByUserIdAndSurfSpotId(Long userId, Long spotId);
}