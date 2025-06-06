package com.lovettj.surfspotsapi.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.*;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.requests.BoundingBox;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SurfSpotService {

    private final SurfSpotRepository surfSpotRepository;
    private final RegionRepository regionRepository;
    private final UserSurfSpotService userSurfSpotService;
    private final WatchListService watchListService;

    public SurfSpotService(SurfSpotRepository surfSpotRepository, RegionRepository regionRepository, UserSurfSpotService userSurfSpotService, WatchListService watchListService) {
        this.surfSpotRepository = surfSpotRepository;
        this.regionRepository = regionRepository;
        this.userSurfSpotService = userSurfSpotService;
        this.watchListService = watchListService;
    }

    public List<SurfSpot> getAllSurfSpots() {
        return surfSpotRepository.findAll();
    }

    public List<SurfSpotDTO> findSurfSpotsWithinBounds(BoundingBox boundingBox, Long userId) {
        List<SurfSpot> surfSpots = surfSpotRepository.findWithinBounds(boundingBox.getMinLatitude(),
                boundingBox.getMaxLatitude(),
                boundingBox.getMinLongitude(),
                boundingBox.getMaxLongitude(), userId);
        return surfSpots.stream()
                .map(surfSpot -> mapToSurfSpotDTO(surfSpot, userId))
                .toList();
    }

    public List<SurfSpotDTO> findSurfSpotsByRegionSlug(String slug, Long userId) {
        Region region = regionRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Region not found"));
        return surfSpotRepository.findByRegion(region, userId).stream().map(surfSpot -> mapToSurfSpotDTO(surfSpot, null)).toList();
    }

    public Optional<SurfSpot> getSurfSpotById(Long id) {
        return surfSpotRepository.findById(id);
    }

    public Optional<SurfSpotDTO> findBySlugAndUserId(String slug, Long userId) {
        return surfSpotRepository.findBySlug(slug, userId)
                .map(surfSpot -> mapToSurfSpotDTO(surfSpot, userId));
    }

    public SurfSpot createSurfSpot(SurfSpotRequest surfSpotRequest) {
        Long userId = surfSpotRequest.getUserId();

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

    private SurfSpotDTO mapToSurfSpotDTO(SurfSpot surfSpot, Long userId) {
        SurfSpotDTO surfSpotDTO = new SurfSpotDTO(surfSpot);
        surfSpotDTO.setPath(generateSurfSpotPath(surfSpot));

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

    private String generateSurfSpotPath(SurfSpot surfSpot) {
        Region region = surfSpot.getRegion();
        Country country = region.getCountry();
        Continent continent = country.getContinent();

        return String.format("/surf-spots/%s/%s/%s/%s",
                continent.getSlug(),
                country.getSlug(),
                region.getSlug(),
                surfSpot.getSlug());
    }
}
