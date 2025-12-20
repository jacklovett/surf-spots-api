package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripMemberRepository extends JpaRepository<TripMember, String> {
    @Query("SELECT tm FROM TripMember tm WHERE tm.trip.id = :tripId AND tm.user.id = :userId")
    Optional<TripMember> findByTripIdAndUserId(@Param("tripId") String tripId, @Param("userId") String userId);

    @Query("SELECT COUNT(tm) FROM TripMember tm WHERE tm.trip.id = :tripId")
    long countByTripId(@Param("tripId") String tripId);

    @Query("SELECT tm FROM TripMember tm WHERE tm.trip.id = :tripId")
    List<TripMember> findByTripId(@Param("tripId") String tripId);

    @Query("SELECT tm FROM TripMember tm WHERE tm.user.id = :userId")
    List<TripMember> findByUserId(@Param("userId") String userId);
}




