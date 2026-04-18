package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurfSessionRepository extends JpaRepository<SurfSession, Long> {
    List<SurfSession> findBySurfSpotId(Long surfSpotId);
    List<SurfSession> findBySurfSpotIdAndSkillLevel(Long surfSpotId, SkillLevel skillLevel);

    @Query("SELECT COUNT(s) FROM SurfSession s WHERE s.user.id = :userId")
    long countAllByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(DISTINCT s.surfSpot.id) FROM SurfSession s WHERE s.user.id = :userId")
    long countDistinctSurfSpotsByUserId(@Param("userId") String userId);

    @Query(
            """
            SELECT COUNT(DISTINCT s.surfboard.id)
            FROM SurfSession s
            WHERE s.user.id = :userId
              AND s.surfboard IS NOT NULL
            """)
    long countDistinctBoardsByUserId(@Param("userId") String userId);

    @Query(
            """
            SELECT DISTINCT s FROM SurfSession s
            JOIN FETCH s.surfSpot sp
            JOIN FETCH sp.region r
            JOIN FETCH r.country c
            JOIN FETCH c.continent
            LEFT JOIN FETCH sp.subRegion
            LEFT JOIN FETCH s.surfboard
            LEFT JOIN FETCH s.media
            WHERE s.user.id = :userId
            ORDER BY s.sessionDate DESC, s.createdAt DESC
            """)
    List<SurfSession> findAllForUserList(@Param("userId") String userId);
}
