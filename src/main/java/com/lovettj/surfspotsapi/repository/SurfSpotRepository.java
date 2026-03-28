package com.lovettj.surfspotsapi.repository;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for SurfSpot entity.
 * Extends JpaRepository for basic CRUD and SurfSpotRepositoryCustom for dynamic filtering.
 */
public interface SurfSpotRepository extends JpaRepository<SurfSpot, Long>, SurfSpotRepositoryCustom {

  List<SurfSpot> findByRegionId(Long regionId);
  boolean existsByRegionIdAndSlug(Long regionId, String slug);
  boolean existsByRegionIdAndSlugAndIdNot(Long regionId, String slug, Long id);
}