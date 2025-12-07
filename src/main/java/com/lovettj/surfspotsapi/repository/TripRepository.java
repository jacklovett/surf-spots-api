package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, String> {
    @Query("SELECT t FROM Trip t WHERE t.owner.id = :userId")
    List<Trip> findByOwnerId(@Param("userId") String userId);

    @Query("SELECT t FROM Trip t JOIN t.members m WHERE m.user.id = :userId")
    List<Trip> findByMemberId(@Param("userId") String userId);

    @Query("SELECT t FROM Trip t WHERE t.owner.id = :userId OR EXISTS (SELECT 1 FROM TripMember tm WHERE tm.trip.id = t.id AND tm.user.id = :userId)")
    List<Trip> findByOwnerIdOrMemberId(@Param("userId") String userId);
}