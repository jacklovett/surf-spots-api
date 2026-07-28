package com.lovettj.surfspotsapi.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;

public interface WatchListRepository extends JpaRepository<WatchListSurfSpot, String> {
  List<WatchListSurfSpot> findByUserId(String userId);

  Optional<WatchListSurfSpot> findByUserIdAndSurfSpotId(String userId, Long surfSpotId);

  @Query("""
          SELECT watchList.surfSpot.id
          FROM WatchListSurfSpot watchList
          WHERE watchList.user.id = :userId
            AND watchList.surfSpot.id IN :spotIds
          """)
  Set<Long> findSurfSpotIdsByUserIdAndSurfSpotIdIn(
          @Param("userId") String userId, @Param("spotIds") Collection<Long> spotIds);
}