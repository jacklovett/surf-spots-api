package com.lovettj.surfspotsapi.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lovettj.surfspotsapi.entity.SurfEvent;
import com.lovettj.surfspotsapi.enums.EventStatus;
import com.lovettj.surfspotsapi.enums.EventType;

public interface SurfEventRepository extends JpaRepository<SurfEvent, Long> {

    @Query("""
            SELECT event FROM SurfEvent event
            JOIN event.contestDetail detail
            WHERE detail.organizer = :organizer
              AND detail.series = :series
              AND detail.venueLocationKey = :venueLocationKey
              AND detail.seasonYear = :seasonYear
            """)
    Optional<SurfEvent> findContestByOrganizerSeriesVenueAndSeasonYear(
            @Param("organizer") String organizer,
            @Param("series") String series,
            @Param("venueLocationKey") String venueLocationKey,
            @Param("seasonYear") int seasonYear);

    @Query("""
            SELECT event FROM SurfEvent event
            JOIN event.contestDetail detail
            WHERE detail.organizer = :organizer
              AND detail.series = :series
              AND detail.venueLocationKey = :venueLocationKey
            """)
    List<SurfEvent> findContestsByOrganizerSeriesAndVenue(
            @Param("organizer") String organizer,
            @Param("series") String series,
            @Param("venueLocationKey") String venueLocationKey);

    Optional<SurfEvent> findFirstByContestDetailOrganizerAndContestDetailSeriesAndContestDetailVenueLocationKeyAndContestDetailSeasonYearLessThanOrderByContestDetailSeasonYearDesc(
            String organizer, String series, String venueLocationKey, Integer seasonYear);

    @Query("""
            SELECT DISTINCT event.surfSpot.id FROM SurfEvent event
            JOIN event.contestDetail detail
            WHERE event.eventType = :eventType
              AND detail.seasonYear = :seasonYear
              AND event.surfSpot IS NOT NULL
              AND event.status NOT IN :excludedStatuses
            """)
    Set<Long> findLinkedSurfSpotIdsForSeasonYearExcludingStatuses(
            @Param("eventType") EventType eventType,
            @Param("seasonYear") int seasonYear,
            @Param("excludedStatuses") Collection<EventStatus> excludedStatuses);

    @Query("""
            SELECT event FROM SurfEvent event
            JOIN event.contestDetail detail
            WHERE event.eventType = :eventType
              AND detail.seasonYear = :seasonYear
              AND event.surfSpot.id IN :surfSpotIds
              AND event.status NOT IN :excludedStatuses
            """)
    List<SurfEvent> findSeasonActiveEventsForYearAndSurfSpotIds(
            @Param("eventType") EventType eventType,
            @Param("seasonYear") int seasonYear,
            @Param("surfSpotIds") Set<Long> surfSpotIds,
            @Param("excludedStatuses") Collection<EventStatus> excludedStatuses);
}
