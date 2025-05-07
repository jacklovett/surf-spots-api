package com.lovettj.surfspotsapi.repository;

import java.util.List;
import java.util.Optional;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for SurfSpot entity.
 * SurfSpot fetching queries fetch only approved or surfspots private to the current user
 */
public interface SurfSpotRepository extends JpaRepository<SurfSpot, Long> {

    @Query("""
        SELECT s FROM SurfSpot s
        WHERE s.region = :region
        AND (
            s.status = 'APPROVED' 
            OR (s.status = 'PRIVATE' AND s.createdBy = :userId)
        )
    """)
    List<SurfSpot> findByRegion(@Param("region") Region region, @Param("userId") Long userId);

    @Query("""
        SELECT s FROM SurfSpot s
        WHERE s.slug = :slug
        AND (
            s.status = 'APPROVED' 
            OR (s.status = 'PRIVATE' AND s.createdBy = :userId)
        )
    """)
    Optional<SurfSpot> findBySlug(@Param("slug") String slug, @Param("userId") Long userId);

    @Query("""
        SELECT s FROM SurfSpot s
        WHERE s.latitude BETWEEN :minLat AND :maxLat
        AND s.longitude BETWEEN :minLon AND :maxLon
        AND (
            s.status = 'APPROVED' 
            OR (s.status = 'PRIVATE' AND s.createdBy = :userId)
        )
    """)
    List<SurfSpot> findWithinBounds(
            @Param("minLat") double minLatitude,
            @Param("maxLat") double maxLatitude,
            @Param("minLon") double minLongitude,
            @Param("maxLon") double maxLongitude,
            @Param("userId") Long userId);
}
