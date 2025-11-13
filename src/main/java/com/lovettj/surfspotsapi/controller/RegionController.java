package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.RegionAndCountryResult;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.service.RegionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {
  private final RegionService regionService;

  public RegionController(RegionService regionService) {
    this.regionService = regionService;
  }

  /**
   * Find region and country by coordinates and country name.
   * This combines the country lookup and region lookup into a single call.
   * 
   * IMPORTANT: This must be defined BEFORE the generic /{regionSlug} route
   * to ensure Spring matches /by-coordinates correctly.
   * Using explicit path value to ensure proper route matching.
   * 
   * @param longitude The longitude coordinate
   * @param latitude The latitude coordinate
   * @param countryName The country name from Mapbox (case-insensitive)
   * @return RegionAndCountryResult containing both region and country, or 404 if country not found
   */
  @GetMapping(value = "/by-coordinates", produces = "application/json")
  public ResponseEntity<RegionAndCountryResult> getRegionAndCountryByCoordinates(
      @RequestParam Double longitude,
      @RequestParam Double latitude,
      @RequestParam String countryName) {
    RegionAndCountryResult result = 
        regionService.findRegionAndCountryByCoordinates(longitude, latitude, countryName);
    if (result == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(result);
  }

  @GetMapping("/country/{countryId}")
  public ResponseEntity<List<Region>> getRegionsByCountryId(@PathVariable Long countryId) {
    List<Region> regions = regionService.getRegionsByCountry(countryId);
    return ResponseEntity.ok(regions);
  }

  @GetMapping("/{countrySlug}/regions")
  public ResponseEntity<List<Region>> getRegionsByCountrySlug(@PathVariable String countrySlug) {
    List<Region> regions = regionService.findRegionsByCountrySlug(countrySlug);
    if (regions.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(regions);
  }

  // This generic route must come LAST to avoid matching /by-coordinates
  // Add explicit check to prevent matching reserved paths
  @GetMapping("/{regionSlug}")
  public ResponseEntity<Region> getRegionBySlug(@PathVariable String regionSlug) {
    // Prevent matching reserved paths like "by-coordinates"
    if ("by-coordinates".equals(regionSlug) || "country".equals(regionSlug)) {
      return ResponseEntity.notFound().build();
    }
    Region region = regionService.getRegionBySlug(regionSlug);
    return ResponseEntity.ok(region);
  }
}
