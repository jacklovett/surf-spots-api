package com.lovettj.surfspotsapi.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.entity.*;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SubRegionRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.requests.BoundingBox;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SurfSpotService {

    private final SurfSpotRepository surfSpotRepository;
    private final RegionRepository regionRepository;
    private final SubRegionRepository subRegionRepository;
    private final UserSurfSpotService userSurfSpotService;
    private final WatchListService watchListService;

    /**
     * Map month names to numeric indices (1-12) for efficient range comparisons.
     * We convert month names to numbers to simplify handling of wrapping ranges
     * (e.g., December to April) which would be more complex with string comparisons.
     * The numeric approach allows straightforward comparison: for wrapping ranges,
     * a month is in range if it's >= start OR <= end.
     */
    private static final Map<String, Integer> MONTH_INDICES = new HashMap<>();
    static {
        MONTH_INDICES.put("january", 1);
        MONTH_INDICES.put("february", 2);
        MONTH_INDICES.put("march", 3);
        MONTH_INDICES.put("april", 4);
        MONTH_INDICES.put("may", 5);
        MONTH_INDICES.put("june", 6);
        MONTH_INDICES.put("july", 7);
        MONTH_INDICES.put("august", 8);
        MONTH_INDICES.put("september", 9);
        MONTH_INDICES.put("october", 10);
        MONTH_INDICES.put("november", 11);
        MONTH_INDICES.put("december", 12);
    }

    public SurfSpotService(SurfSpotRepository surfSpotRepository, RegionRepository regionRepository, SubRegionRepository subRegionRepository, UserSurfSpotService userSurfSpotService, WatchListService watchListService) {
        this.surfSpotRepository = surfSpotRepository;
        this.regionRepository = regionRepository;
        this.subRegionRepository = subRegionRepository;
        this.userSurfSpotService = userSurfSpotService;
        this.watchListService = watchListService;
    }

    /**
     * Converts a month name to its numeric index (1-12)
     */
    private Integer getMonthIndex(String monthName) {
        if (monthName == null) return null;
        return MONTH_INDICES.get(monthName.toLowerCase());
    }

    /**
     * Checks if a selected month falls within a season range.
     * Handles both normal ranges (e.g., March-June) and wrapping ranges (e.g., December-April).
     * Uses numeric indices for efficient comparison of month ranges.
     */
    private boolean isMonthInSeasonRange(String selectedMonth, String seasonStart, String seasonEnd) {
        Integer selectedIndex = getMonthIndex(selectedMonth);
        Integer startIndex = getMonthIndex(seasonStart);
        Integer endIndex = getMonthIndex(seasonEnd);

        if (selectedIndex == null || startIndex == null || endIndex == null) {
            return false;
        }

        // Case 1: Normal range (start <= end), e.g., March (3) - June (6)
        if (startIndex <= endIndex) {
            return selectedIndex >= startIndex && selectedIndex <= endIndex;
        }
        // Case 2: Wrapping range (start > end), e.g., December (12) - April (4)
        // Selected month must be >= start OR <= end
        else {
            return selectedIndex >= startIndex || selectedIndex <= endIndex;
        }
    }

    /**
     * Filters surf spots by season if season filter is provided
     */
    private List<SurfSpot> filterBySeason(List<SurfSpot> surfSpots, SurfSpotFilterDTO filters) {
        if (filters.getSeasons() == null || filters.getSeasons().isEmpty()) {
            return surfSpots;
        }

        return surfSpots.stream()
                .filter(spot -> {
                    String seasonStart = spot.getSeasonStart();
                    String seasonEnd = spot.getSeasonEnd();
                    
                    if (seasonStart == null || seasonEnd == null) {
                        return false;
                    }

                    // Check if any selected month falls within this spot's season range
                    return filters.getSeasons().stream()
                            .anyMatch(selectedMonth -> isMonthInSeasonRange(selectedMonth, seasonStart, seasonEnd));
                })
                .collect(Collectors.toList());
    }


    public Optional<SurfSpot> getSurfSpotById(Long id) {
        return surfSpotRepository.findById(id);
    }

    public Optional<SurfSpotDTO> findBySlugAndUserId(String slug, String userId) {
        Optional<SurfSpot> surfSpot = Optional.ofNullable(surfSpotRepository.findBySlug(slug, userId));
        return surfSpot.map(sp -> mapToSurfSpotDTO(sp, userId));
    }

    public SurfSpot createSurfSpot(SurfSpotRequest surfSpotRequest) {
        String userId = surfSpotRequest.getUserId();

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to identifiy user");
        }
        
        // Create a new SurfSpot entity
        SurfSpot surfSpot = new SurfSpot();
        surfSpot.setCreatedBy(userId);
        // Set basic fields
        surfSpot.setName(surfSpotRequest.getName());
        surfSpot.setDescription(surfSpotRequest.getDescription());
        surfSpot.setLatitude(surfSpotRequest.getLatitude());
        surfSpot.setLongitude(surfSpotRequest.getLongitude());
        surfSpot.setSwellDirection(surfSpotRequest.getSwellDirection());
        surfSpot.setWindDirection(surfSpotRequest.getWindDirection());
        surfSpot.setMinSurfHeight(surfSpotRequest.getMinSurfHeight());
        surfSpot.setMaxSurfHeight(surfSpotRequest.getMaxSurfHeight());
        surfSpot.setSeasonStart(surfSpotRequest.getSeasonStart());
        surfSpot.setSeasonEnd(surfSpotRequest.getSeasonEnd());
        surfSpot.setRating(surfSpotRequest.getRating());
        surfSpot.setForecasts(surfSpotRequest.getForecasts());

        // Enums
        surfSpot.setType(surfSpotRequest.getType());
        surfSpot.setBeachBottomType(surfSpotRequest.getBeachBottomType());
        surfSpot.setSkillLevel(surfSpotRequest.getSkillLevel());
        surfSpot.setTide(surfSpotRequest.getTide());
        surfSpot.setWaveDirection(surfSpotRequest.getWaveDirection());
        surfSpot.setParking(surfSpotRequest.getParking());
        surfSpot.setStatus(surfSpotRequest.getStatus());

        // Handle boolean fields
        surfSpot.setBoatRequired(surfSpotRequest.isBoatRequired());
        surfSpot.setAccommodationNearby(surfSpotRequest.isAccommodationNearby());
        surfSpot.setFoodNearby(surfSpotRequest.isFoodNearby());

        // Handle arrays of enums
        surfSpot.setHazards(Optional.ofNullable(surfSpotRequest.getHazards())
        .orElse(Collections.emptyList()));
        surfSpot.setFoodOptions(Optional.ofNullable(surfSpotRequest.getFoodOptions()).orElse(Collections.emptyList()));
        surfSpot.setAccommodationOptions(Optional.ofNullable(surfSpotRequest.getAccommodationOptions()).orElse(Collections.emptyList()));
        surfSpot.setFacilities(Optional.ofNullable(surfSpotRequest.getFacilities()).orElse(Collections.emptyList()));

        // Fetch and set related entities (region, country, continent)
        Region region = regionRepository.findById(surfSpotRequest.getRegionId())
                .orElseThrow(() -> new EntityNotFoundException("Region not found"));
        surfSpot.setRegion(region);

        // Save the SurfSpot entity
        return surfSpotRepository.save(surfSpot);
    }

    public SurfSpot updateSurfSpot(Long id, SurfSpot updatedSurfSpot) {
        if (surfSpotRepository.existsById(id)) {
            updatedSurfSpot.setId(id);
            return surfSpotRepository.save(updatedSurfSpot);
        } else {
            throw new EntityNotFoundException("SurfSpot not found");
        }
    }

    public void deleteSurfSpot(Long id) {
        surfSpotRepository.deleteById(id);
    }

    public List<SurfSpotDTO> findSurfSpotsWithinBoundsWithFilters(BoundingBox boundingBox, SurfSpotBoundsFilterDTO filters) {
        List<SurfSpot> surfSpots = surfSpotRepository.findWithinBoundsWithFilters(
                filters);
        // Filter by season if needed
        surfSpots = filterBySeason(surfSpots, filters);
        return surfSpots.stream()
                .map(surfSpot -> mapToSurfSpotDTO(surfSpot, filters.getUserId()))
                .toList();
    }

    public List<SurfSpotDTO> findSurfSpotsByRegionSlugWithFilters(String slug, SurfSpotFilterDTO filters) {
        Region region = regionRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Region not found"));
        List<SurfSpot> surfSpots = surfSpotRepository.findByRegionWithFilters(region, filters);
        // Filter by season if needed
        surfSpots = filterBySeason(surfSpots, filters);
        return surfSpots.stream()
                .map(surfSpot -> mapToSurfSpotDTO(surfSpot, filters.getUserId()))
                .toList();
    }

    public List<SurfSpotDTO> findSurfSpotsBySubRegionSlugWithFilters(String slug, SurfSpotFilterDTO filters) {
        SubRegion subRegion = subRegionRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("SubRegion not found"));
        List<SurfSpot> surfSpots = surfSpotRepository.findBySubRegionWithFilters(subRegion, filters);
        // Filter by season if needed
        surfSpots = filterBySeason(surfSpots, filters);
        return surfSpots.stream()
                .map(surfSpot -> mapToSurfSpotDTO(surfSpot, filters.getUserId()))
                .toList();
    }

    private SurfSpotDTO mapToSurfSpotDTO(SurfSpot surfSpot, String userId) {
        // SurfSpotDTO constructor now sets the path automatically
        SurfSpotDTO surfSpotDTO = new SurfSpotDTO(surfSpot);

        if (userId != null) {
            Long surfSpotId = surfSpot.getId();
            // Check if this is a users surfed spot or in the Watchlist
            boolean isSurfedSpot = userSurfSpotService.isUserSurfedSpot(userId, surfSpotId);
            boolean isWatched = watchListService.isWatched(userId, surfSpotId);
            surfSpotDTO.setIsSurfedSpot(isSurfedSpot);
            surfSpotDTO.setIsWatched(isWatched);
        }

        return surfSpotDTO;
    }
}
