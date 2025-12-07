package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.TripMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripMediaRepository extends JpaRepository<TripMedia, String> {
    @Query("SELECT tm FROM TripMedia tm WHERE tm.trip.id = :tripId ORDER BY tm.uploadedAt DESC")
    List<TripMedia> findByTripIdOrderByUploadedAtDesc(@Param("tripId") String tripId);

    @Query("SELECT COUNT(tm) FROM TripMedia tm WHERE tm.trip.id = :tripId AND tm.owner.id = :userId")
    long countByTripIdAndOwnerId(@Param("tripId") String tripId, @Param("userId") String userId);
}