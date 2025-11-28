package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.RegionAndCountryResult;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;

import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RegionService {
  private static final Logger logger = LoggerFactory.getLogger(RegionService.class);
  
  private final CountryRepository countryRepository;
  private final RegionRepository regionRepository;

  public RegionService(RegionRepository regionRepository, CountryRepository countryRepository) {
    this.countryRepository = countryRepository;
    this.regionRepository = regionRepository;
  }

  public Region getRegionBySlug(String slug) {
    return regionRepository.findBySlug(slug).orElseThrow(() -> new EntityNotFoundException("Region not found"));
  }

  public List<Region> getRegionsByCountry(Long countryId) {
    return regionRepository.findByCountryId(countryId);
  }

  public List<Region> findRegionsByCountrySlug(String slug) {
    Country country = countryRepository.findBySlug(slug)
        .orElseThrow(() -> new EntityNotFoundException("Country not found"));
    return regionRepository.findByCountryId(country.getId());
  }

  /**
   * Find region that contains the given coordinates (longitude, latitude)
   * 
   * Strategy:
   * 1. Try bounding box array check for exact matches (point inside bounding box)
   * 2. If no exact match, try buffer distance for nearby regions (handles edge cases)
   * 3. Fallback to closest surf spot for regions without bounding boxes
   * 
   * @param longitude The longitude coordinate
   * @param latitude The latitude coordinate
   * @param countryId Optional country ID to filter regions by country first (performance optimization)
   * @return The matching region, or null if no region found
   */
  @Cacheable(value = "regionLookup", key = "#longitude + '_' + #latitude + '_' + (#countryId != null ? #countryId : 'null')", unless = "#result == null")
  public Region findRegionByCoordinates(Double longitude, Double latitude, Long countryId) {
    // Step 1: Try bounding box array check for exact matches
    try {
      Optional<Region> exactMatch = regionRepository.findRegionContainingPoint(longitude, latitude, countryId);
      
      if (exactMatch.isPresent()) {
        return exactMatch.get();
      }
    } catch (Exception e) {
      logger.warn("Bounding box query failed: {}", e.getMessage());
    }
    
    // Step 2: Try buffer distance for nearby regions
    try {
      double bufferDistanceDegrees = 0.045; // ~5km at equator
      Optional<Region> nearbyMatch = regionRepository.findRegionNearPoint(longitude, latitude, bufferDistanceDegrees, countryId);
      
      if (nearbyMatch.isPresent()) {
        return nearbyMatch.get();
      }
    } catch (Exception e) {
      logger.warn("Buffer query failed: {}", e.getMessage());
    }
    
    // Step 3: Fallback to closest surf spot for regions without bounding boxes
    List<Region> regionsToCheck = regionRepository.findAllWithSurfSpots(countryId);
    
    Region closestRegion = null;
    double minDistance = Double.MAX_VALUE;
    
    for (Region region : regionsToCheck) {
      // Skip regions with bounding boxes (already checked in Step 1)
      if (region.getBoundingBox() != null && region.getBoundingBox().length == 4) {
        continue;
      }
      
      // For regions without bounding boxes, use closest surf spot
      if (region.getSurfSpots() != null && !region.getSurfSpots().isEmpty()) {
        for (var surfSpot : region.getSurfSpots()) {
          if (surfSpot.getLatitude() != null && surfSpot.getLongitude() != null) {
            double distance = calculateDistance(
                latitude, longitude,
                surfSpot.getLatitude(), surfSpot.getLongitude()
            );
            if (distance < minDistance) {
              minDistance = distance;
              closestRegion = region;
            }
          }
        }
      }
    }
    
    if (closestRegion == null) {
      logger.warn("No region found for coordinates: {}, {}", longitude, latitude);
    }
    
    return closestRegion;
  }

  /**
   * Find region that contains the given coordinates (longitude, latitude)
   * Convenience method without country filter.
   */
  public Region findRegionByCoordinates(Double longitude, Double latitude) {
    return findRegionByCoordinates(longitude, latitude, null);
  }

  /**
   * Find region and country by coordinates and country name.
   * First finds the country by name, then finds the region using the country ID.
   * 
   * @param longitude The longitude coordinate
   * @param latitude The latitude coordinate
   * @param countryName The country name from Mapbox (case-insensitive)
   * @return A RegionAndCountryResult containing the region and country, or null if country not found
   */
  public RegionAndCountryResult findRegionAndCountryByCoordinates(
      Double longitude, Double latitude, String countryName) {
    // First, find the country by name
    Optional<Country> countryOpt = countryRepository.findByNameIgnoreCase(countryName);
    if (countryOpt.isEmpty()) {
      logger.warn("Country '{}' not found in database", countryName);
      return null;
    }
    
    Country country = countryOpt.get();
    if (country.getContinent() != null) {
      country.getContinent().getSlug();
    }
    Long countryId = country.getId();
    
    // Then find the region using the country ID
    Region region = findRegionByCoordinates(longitude, latitude, countryId);
    
    return new RegionAndCountryResult(region, country);
  }

  /**
   * Calculate distance between two points using Haversine formula
   * Returns distance in kilometers
   */
  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371; // Radius of the earth in km
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
}
