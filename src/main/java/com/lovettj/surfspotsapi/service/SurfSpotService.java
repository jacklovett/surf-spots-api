package com.lovettj.surfspotsapi.service;

import java.time.Month;
import java.util.Collections;
import java.util.List;
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
import com.lovettj.surfspotsapi.util.MonthUtils;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SurfSpotService {

    private final SurfSpotRepository surfSpotRepository;
    private final RegionRepository regionRepository;
    private final SubRegionRepository subRegionRepository;
    private final UserSurfSpotService userSurfSpotService;
    private final WatchListService watchListService;
    private final SwellSeasonDeterminationService swellSeasonDeterminationService;

    public SurfSpotService(SurfSpotRepository surfSpotRepository, RegionRepository regionRepository, SubRegionRepository subRegionRepository, UserSurfSpotService userSurfSpotService, WatchListService watchListService, SwellSeasonDeterminationService swellSeasonDeterminationService) {
        this.surfSpotRepository = surfSpotRepository;
        this.regionRepository = regionRepository;
        this.subRegionRepository = subRegionRepository;
        this.userSurfSpotService = userSurfSpotService;
        this.watchListService = watchListService;
        this.swellSeasonDeterminationService = swellSeasonDeterminationService;
    }

    /**
     * Checks if a selected month falls within a season range using month names.
     * Handles both normal ranges (e.g., March-June) and wrapping ranges (e.g., December-April).
     */
    private boolean isMonthInSeasonRange(String selectedMonth, String startMonth, String endMonth) {
        Month selected = MonthUtils.parseMonthString(selectedMonth);
        Month start = MonthUtils.parseMonthString(startMonth);
        Month end = MonthUtils.parseMonthString(endMonth);
        
        if (selected == null || start == null || end == null) {
            return false;
        }

        int selectedValue = selected.getValue();
        int startValue = start.getValue();
        int endValue = end.getValue();

        // Case 1: Normal range (start <= end), e.g., March (3) - June (6)
        if (startValue <= endValue) {
            return selectedValue >= startValue && selectedValue <= endValue;
        }
        // Case 2: Wrapping range (start > end), e.g., December (12) - April (4)
        // Selected month must be >= start OR <= end
        else {
            return selectedValue >= startValue || selectedValue <= endValue;
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
                    if (spot.getSwellSeason() == null) {
                        return false;
                    }

                    String start = spot.getSwellSeason().getStartMonth();
                    String end = spot.getSwellSeason().getEndMonth();
                    
                    if (start == null || end == null) {
                        return false;
                    }

                    // Check if any selected month falls within this spot's season range
                    return filters.getSeasons().stream()
                            .anyMatch(selectedMonth -> isMonthInSeasonRange(selectedMonth, start, end));
                })
                .collect(Collectors.toList());
    }

    public Optional<SurfSpotDTO> findByIdAndUserId(Long id, String userId) {
        Optional<SurfSpot> surfSpot = surfSpotRepository.findById(id);
        return surfSpot.map(sp -> mapToSurfSpotDTO(sp, userId));
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
        surfSpot.setIsWavepool(surfSpotRequest.isWavepool());
        surfSpot.setWavepoolUrl(surfSpotRequest.getWavepoolUrl());
        surfSpot.setIsRiverWave(surfSpotRequest.isRiverWave());
        surfSpot.setAccommodationNearby(surfSpotRequest.isAccommodationNearby());
        surfSpot.setFoodNearby(surfSpotRequest.isFoodNearby());

        // Handle arrays of enums
        surfSpot.setHazards(Optional.ofNullable(surfSpotRequest.getHazards())
        .orElse(Collections.emptyList()));
        surfSpot.setFoodOptions(Optional.ofNullable(surfSpotRequest.getFoodOptions()).orElse(Collections.emptyList()));
        surfSpot.setAccommodationOptions(Optional.ofNullable(surfSpotRequest.getAccommodationOptions()).orElse(Collections.emptyList()));
        surfSpot.setFacilities(Optional.ofNullable(surfSpotRequest.getFacilities()).orElse(Collections.emptyList()));

        // Fetch and set related entities (region, country, continent)
        Long regionId = surfSpotRequest.getRegionId();
        if (regionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Region ID is required");
        }
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new EntityNotFoundException("Region not found"));
        surfSpot.setRegion(region);

        // Automatically determine swell season based on coordinates
        // Skip for wavepools and river waves as they don't have natural swell seasons
        // This is done at surf spot level because regions can have multiple coastlines
        // (e.g., Andalusia has both Mediterranean and Atlantic coasts)
        if (!Boolean.TRUE.equals(surfSpot.getIsWavepool()) && !Boolean.TRUE.equals(surfSpot.getIsRiverWave())) {
            swellSeasonDeterminationService.determineSwellSeason(
                    surfSpot.getLatitude(), 
                    surfSpot.getLongitude()
            ).ifPresent(surfSpot::setSwellSeason);
        }

        // Save the SurfSpot entity
        return surfSpotRepository.save(surfSpot);
    }

    public SurfSpot updateSurfSpot(Long id, SurfSpotRequest surfSpotRequest) {
        SurfSpot existingSurfSpot = surfSpotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SurfSpot not found"));
        
        // Update basic fields
        existingSurfSpot.setName(surfSpotRequest.getName());
        existingSurfSpot.setDescription(surfSpotRequest.getDescription());
        existingSurfSpot.setLatitude(surfSpotRequest.getLatitude());
        existingSurfSpot.setLongitude(surfSpotRequest.getLongitude());
        existingSurfSpot.setSwellDirection(surfSpotRequest.getSwellDirection());
        existingSurfSpot.setWindDirection(surfSpotRequest.getWindDirection());
        existingSurfSpot.setMinSurfHeight(surfSpotRequest.getMinSurfHeight());
        existingSurfSpot.setMaxSurfHeight(surfSpotRequest.getMaxSurfHeight());
        existingSurfSpot.setRating(surfSpotRequest.getRating());
        existingSurfSpot.setForecasts(surfSpotRequest.getForecasts());

        // Update enums
        existingSurfSpot.setType(surfSpotRequest.getType());
        existingSurfSpot.setBeachBottomType(surfSpotRequest.getBeachBottomType());
        existingSurfSpot.setSkillLevel(surfSpotRequest.getSkillLevel());
        existingSurfSpot.setTide(surfSpotRequest.getTide());
        existingSurfSpot.setWaveDirection(surfSpotRequest.getWaveDirection());
        existingSurfSpot.setParking(surfSpotRequest.getParking());
        existingSurfSpot.setStatus(surfSpotRequest.getStatus());

        // Update boolean fields
        existingSurfSpot.setBoatRequired(surfSpotRequest.isBoatRequired());
        existingSurfSpot.setIsWavepool(surfSpotRequest.isWavepool());
        existingSurfSpot.setWavepoolUrl(surfSpotRequest.getWavepoolUrl());
        existingSurfSpot.setIsRiverWave(surfSpotRequest.isRiverWave());
        existingSurfSpot.setAccommodationNearby(surfSpotRequest.isAccommodationNearby());
        existingSurfSpot.setFoodNearby(surfSpotRequest.isFoodNearby());

        // Update arrays of enums
        existingSurfSpot.setHazards(Optional.ofNullable(surfSpotRequest.getHazards())
                .orElse(Collections.emptyList()));
        existingSurfSpot.setFoodOptions(Optional.ofNullable(surfSpotRequest.getFoodOptions())
                .orElse(Collections.emptyList()));
        existingSurfSpot.setAccommodationOptions(Optional.ofNullable(surfSpotRequest.getAccommodationOptions())
                .orElse(Collections.emptyList()));
        existingSurfSpot.setFacilities(Optional.ofNullable(surfSpotRequest.getFacilities())
                .orElse(Collections.emptyList()));

        // Update region if provided
        Long regionId = surfSpotRequest.getRegionId();
        if (regionId != null) {
            Region region = regionRepository.findById(regionId)
                    .orElseThrow(() -> new EntityNotFoundException("Region not found"));
            existingSurfSpot.setRegion(region);
        }

        // Automatically determine and update swell season based on coordinates
        // Skip for wavepools and river waves as they don't have natural swell seasons
        // This is done at surf spot level because regions can have multiple coastlines
        if (!Boolean.TRUE.equals(existingSurfSpot.getIsWavepool()) && !Boolean.TRUE.equals(existingSurfSpot.getIsRiverWave())) {
            swellSeasonDeterminationService.determineSwellSeason(
                    existingSurfSpot.getLatitude(), 
                    existingSurfSpot.getLongitude()
            ).ifPresent(existingSurfSpot::setSwellSeason);
        } else {
            // Clear swell season for wavepools and river waves
            existingSurfSpot.setSwellSeason(null);
        }

        // Save and return the updated entity
        return surfSpotRepository.save(existingSurfSpot);
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

    public SurfSpotDTO mapToSurfSpotDTO(SurfSpot surfSpot, String userId) {
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
