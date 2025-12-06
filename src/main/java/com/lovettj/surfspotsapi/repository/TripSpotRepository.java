package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.TripSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripSpotRepository extends JpaRepository<TripSpot, String> {
    @Query("SELECT ts FROM TripSpot ts WHERE ts.trip.id = :tripId")
    List<TripSpot> findByTripId(@Param("tripId") String tripId);

    @Query("SELECT ts FROM TripSpot ts WHERE ts.trip.id = :tripId AND ts.surfSpot.id = :surfSpotId")
    Optional<TripSpot> findByTripIdAndSurfSpotId(@Param("tripId") String tripId, @Param("surfSpotId") Long surfSpotId);
}







