package com.lovettj.surfspotsapi.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.requests.BoundingBox;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SurfSpotService {
  private final SurfSpotRepository surfSpotRepository;
  private final RegionRepository regionRepository;
  private final UserSurfSpotService userSurfSpotService;
  private final WatchListService watchListService;

  public SurfSpotService(SurfSpotRepository surfSpotRepository, RegionRepository regionRepository, UserSurfSpotService userSurfSpotService,  WatchListService watchListService) {
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
        boundingBox.getMaxLongitude());
    return surfSpots.stream()
        .map(surfSpot -> mapToSurfSpotDTO(surfSpot, userId))
        .collect(Collectors.toList());
  }

  public List<SurfSpotDTO> findSurfSpotsByRegionSlug(String slug) {
    Region region = regionRepository.findBySlug(slug)
        .orElseThrow(() -> new EntityNotFoundException("Region not found"));
    return surfSpotRepository.findByRegion(region).stream().map(surfSpot -> mapToSurfSpotDTO(surfSpot, null)).collect(Collectors.toList());
  }

  public Optional<SurfSpot> getSurfSpotById(Long id) {
    return surfSpotRepository.findById(id);
  }

  public Optional<SurfSpotDTO> findBySlugAndUserId(String slug, Long userId) {
      return surfSpotRepository.findBySlug(slug)
          .map(surfSpot -> mapToSurfSpotDTO(surfSpot, userId));
  }

  public SurfSpot createSurfSpot(SurfSpot surfSpot) {
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