package com.lovettj.surfspotsapi.repository;

import java.util.List;

import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.entity.SurfSpot;

public interface SurfSpotRepositoryCustom {
    /**
     * @return the matching surf spot, or {@code null} if none (slug + optional location + visibility rules for userId).
     */
    SurfSpot findBySlug(String slug, String userId, String countrySlug, String regionSlug);
    List<SurfSpot> findByRegionWithFilters(Region region, SurfSpotFilterDTO filters);
    List<SurfSpot> findBySubRegionWithFilters(SubRegion subRegion, SurfSpotFilterDTO filters);
    List<SurfSpot> findWithinBoundsWithFilters(SurfSpotBoundsFilterDTO filters);
}
