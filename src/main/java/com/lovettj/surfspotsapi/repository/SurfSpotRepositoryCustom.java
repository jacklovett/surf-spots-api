package com.lovettj.surfspotsapi.repository;

import java.util.List;

import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;

public interface SurfSpotRepositoryCustom {
    SurfSpot findBySlug(String slug, String userId);
    List<SurfSpot> findByRegionWithFilters(Region region, SurfSpotFilterDTO filters);
    List<SurfSpot> findWithinBoundsWithFilters(SurfSpotBoundsFilterDTO filters);
}
