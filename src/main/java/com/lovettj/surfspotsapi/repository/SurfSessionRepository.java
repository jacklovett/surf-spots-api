package com.lovettj.surfspotsapi.repository;

import jakarta.persistence.LockModeType;

import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.enums.ExternalSessionProvider;
import com.lovettj.surfspotsapi.enums.SessionStatus;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SurfSessionRepository extends JpaRepository<SurfSession, Long> {
    @Query(
            """
            SELECT COUNT(session) > 0
            FROM SurfSession session
            WHERE session.user.id = :userId
              AND session.externalSessionProvider = :externalSessionProvider
              AND session.externalSessionId = :externalSessionId
            """)
    boolean externalSessionAlreadyRecordedForUser(
            @Param("userId") String userId,
            @Param("externalSessionProvider") ExternalSessionProvider externalSessionProvider,
            @Param("externalSessionId") String externalSessionId);

    List<SurfSession> findBySurfSpotId(Long surfSpotId);
    List<SurfSession> findBySurfSpotIdAndSkillLevel(Long surfSpotId, SkillLevel skillLevel);

    @Query(
            """
            SELECT session FROM SurfSession session
            WHERE session.user.id = :userId
              AND session.surfSpot IS NULL
              AND session.status <> :inProgressStatus
              AND session.startLatitude IS NOT NULL
              AND session.startLongitude IS NOT NULL
            """)
    List<SurfSession> findUnassignedWithStartLocationByUserIdExcludingInProgress(
            @Param("userId") String userId, @Param("inProgressStatus") SessionStatus inProgressStatus);

    @Query(
            """
            SELECT COUNT(session) > 0
            FROM SurfSession session
            WHERE session.user.id = :userId
              AND session.status = :status
            """)
    boolean existsByUserIdAndStatus(
            @Param("userId") String userId, @Param("status") SessionStatus status);

    @Query(
            """
            SELECT session FROM SurfSession session
            WHERE session.user.id = :userId
              AND session.status = :status
            ORDER BY session.sessionStartInstant DESC, session.createdAt DESC
            """)
    Optional<SurfSession> findFirstByUserIdAndStatusOrderBySessionStartInstantDescCreatedAtDesc(
            @Param("userId") String userId, @Param("status") SessionStatus status);

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
            LEFT JOIN FETCH s.surfSpot sp
            LEFT JOIN FETCH sp.region r
            LEFT JOIN FETCH r.country c
            LEFT JOIN FETCH c.continent
            LEFT JOIN FETCH sp.subRegion
            LEFT JOIN FETCH s.surfboard
            LEFT JOIN FETCH s.media
            WHERE s.user.id = :userId
            ORDER BY s.sessionDate DESC, s.createdAt DESC
            """)
    List<SurfSession> findAllForUserList(@Param("userId") String userId);

    @Query(
            """
            SELECT session.id FROM SurfSession session
            WHERE session.status = :status
              AND session.shareLocationWithEmergencyContact = true
              AND session.expectedReturnInstant IS NOT NULL
              AND session.expectedReturnInstant < :now
              AND session.overdueNotificationSentAt IS NULL
            """)
    List<Long> findOverdueLiveSessionIdsNeedingNotification(
            @Param("status") SessionStatus status, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT session FROM SurfSession session
            JOIN FETCH session.user user
            WHERE session.id = :sessionId
            """)
    Optional<SurfSession> findByIdWithUserForUpdate(@Param("sessionId") Long sessionId);
}
