package com.lovettj.surfspotsapi.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lovettj.surfspotsapi.entity.EnvironmentalAlert;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertStatus;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertType;

public interface EnvironmentalAlertRepository extends JpaRepository<EnvironmentalAlert, Long> {

    @Query("""
            SELECT alert FROM EnvironmentalAlert alert
            JOIN FETCH alert.surfSpot
            WHERE alert.surfSpot.id IN :surfSpotIds
              AND alert.status = :status
            ORDER BY alert.detectedAt DESC
            """)
    List<EnvironmentalAlert> findBySurfSpotIdInAndStatusOrderByDetectedAtDesc(
            @Param("surfSpotIds") Collection<Long> surfSpotIds, @Param("status") EnvironmentalAlertStatus status);

    Optional<EnvironmentalAlert> findBySurfSpotIdAndTypeAndExternalIdAndStatus(
            Long surfSpotId, EnvironmentalAlertType type, String externalId, EnvironmentalAlertStatus status);

    @Query("""
            SELECT alert FROM EnvironmentalAlert alert
            WHERE alert.status = :status
              AND alert.expiresAt IS NOT NULL
              AND alert.expiresAt < :now
            """)
    List<EnvironmentalAlert> findExpiredActiveAlerts(
            @Param("status") EnvironmentalAlertStatus status, @Param("now") Instant now);
}
