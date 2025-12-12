package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.TripSurfboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripSurfboardRepository extends JpaRepository<TripSurfboard, String> {
    @Query("SELECT ts FROM TripSurfboard ts WHERE ts.trip.id = :tripId ORDER BY ts.addedAt ASC")
    List<TripSurfboard> findByTripId(@Param("tripId") String tripId);
}