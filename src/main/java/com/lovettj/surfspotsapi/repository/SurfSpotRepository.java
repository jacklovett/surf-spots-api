package com.lovettj.surfspotsapi.repository;

import java.util.List;
import java.util.Optional;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurfSpotRepository extends JpaRepository<SurfSpot, Long> {

    List<SurfSpot> findByRegion(Region region);

    Optional<SurfSpot> findBySlug(String slug);

    // Find surf spots within a bounding box (min/max lat/lon)
    @Query("SELECT s FROM SurfSpot s WHERE s.latitude BETWEEN :minLat AND :maxLat AND s.longitude BETWEEN :minLon AND :maxLon")
    List<SurfSpot> findWithinBounds(
            @Param("minLat") double minLatitude,
            @Param("maxLat") double maxLatitude,
            @Param("minLon") double minLongitude,
            @Param("maxLon") double maxLongitude);
}
