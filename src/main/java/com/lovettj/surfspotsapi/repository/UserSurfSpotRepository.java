package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.UserSurfSpot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSurfSpotRepository extends JpaRepository<UserSurfSpot, String> {
    Optional<UserSurfSpot> findByUserIdAndSurfSpotId(String userId, Long spotId);
    List<UserSurfSpot> findByUserId(String userId);

    List<UserSurfSpot> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("""
            SELECT userSurfSpot.surfSpot.id
            FROM UserSurfSpot userSurfSpot
            WHERE userSurfSpot.user.id = :userId
              AND userSurfSpot.surfSpot.id IN :spotIds
            """)
    Set<Long> findSurfSpotIdsByUserIdAndSurfSpotIdIn(
            @Param("userId") String userId, @Param("spotIds") Collection<Long> spotIds);
}
