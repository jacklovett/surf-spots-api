package com.lovettj.surfspotsapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;

public interface WatchListRepository extends JpaRepository<WatchListSurfSpot, String> {
  List<WatchListSurfSpot> findByUserId(String userId);

  Optional<WatchListSurfSpot> findByUserIdAndSurfSpotId(String userId, Long surfSpotId);
}