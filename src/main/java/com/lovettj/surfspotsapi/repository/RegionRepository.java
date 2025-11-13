package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionRepository extends JpaRepository<Region, Long> {
  Optional<Region> findBySlug(String slug);

  List<Region> findByCountryId(Long countryId);

  /**
   * Find regions that contain the given point using bounding box array check.
   * If countryId is provided, filters by country.
   */
  @Query(value = """
      SELECT r.* FROM region r
      WHERE (:countryId IS NULL OR r.country_id = :countryId)
        AND r.bounding_box IS NOT NULL
        AND array_length(r.bounding_box, 1) = 4
        AND :longitude >= r.bounding_box[1] -- minLongitude
        AND :longitude <= r.bounding_box[3] -- maxLongitude
        AND :latitude >= r.bounding_box[2]  -- minLatitude
        AND :latitude <= r.bounding_box[4]  -- maxLatitude
      ORDER BY (r.bounding_box[3] - r.bounding_box[1]) * (r.bounding_box[4] - r.bounding_box[2]) ASC
      LIMIT 1
      """, nativeQuery = true)
  Optional<Region> findRegionContainingPoint(
      @Param("longitude") Double longitude,
      @Param("latitude") Double latitude,
      @Param("countryId") Long countryId);

  /**
   * Find regions near the given point (within a buffer distance) when point is not inside any bounding box.
   * If countryId is provided, filters by country.
   */
  @Query(value = """
      SELECT r.* FROM region r
      WHERE (:countryId IS NULL OR r.country_id = :countryId)
        AND r.bounding_box IS NOT NULL
        AND array_length(r.bounding_box, 1) = 4
        AND (
          (:longitude >= r.bounding_box[1] - :bufferDistance AND :longitude <= r.bounding_box[3] + :bufferDistance)
          AND (:latitude >= r.bounding_box[2] - :bufferDistance AND :latitude <= r.bounding_box[4] + :bufferDistance)
        )
      ORDER BY 
        ABS((r.bounding_box[1] + r.bounding_box[3]) / 2 - :longitude) + 
        ABS((r.bounding_box[2] + r.bounding_box[4]) / 2 - :latitude) ASC,
        (r.bounding_box[3] - r.bounding_box[1]) * (r.bounding_box[4] - r.bounding_box[2]) ASC
      LIMIT 1
      """, nativeQuery = true)
  Optional<Region> findRegionNearPoint(
      @Param("longitude") Double longitude,
      @Param("latitude") Double latitude,
      @Param("bufferDistance") Double bufferDistance,
      @Param("countryId") Long countryId);

  /**
   * Find regions with surf spots eagerly fetched (for fallback method).
   * If countryId is provided, filters by country.
   * Uses JOIN FETCH to eagerly load surf spots to avoid N+1 queries.
   */
  @Query("SELECT DISTINCT r FROM Region r LEFT JOIN FETCH r.surfSpots WHERE (:countryId IS NULL OR r.country.id = :countryId)")
  List<Region> findAllWithSurfSpots(@Param("countryId") Long countryId);
}
