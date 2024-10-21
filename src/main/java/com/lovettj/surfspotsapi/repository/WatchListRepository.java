package com.lovettj.surfspotsapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;

public interface WatchListRepository extends JpaRepository<WatchListSurfSpot, Long> {
  List<WatchListSurfSpot> findByUserId(Long userId);

  Optional<WatchListSurfSpot> findByUserIdAndSurfSpotId(Long userId, Long spotId);
}