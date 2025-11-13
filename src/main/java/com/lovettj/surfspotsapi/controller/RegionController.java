package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.RegionAndCountryResult;
import com.lovettj.surfspotsapi.dto.RegionLookupRequest;
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
   * Using POST to avoid conflict with GET /{regionSlug} route.
   * This ensures Spring matches the correct route handler.
   * 
   * @param request The lookup request containing longitude, latitude, and countryName
   * @return RegionAndCountryResult containing both region and country, or 404 if country not found
   */
  @PostMapping("/by-coordinates")
  public ResponseEntity<RegionAndCountryResult> getRegionAndCountryByCoordinates(
      @RequestBody RegionLookupRequest request) {
    RegionAndCountryResult result = 
        regionService.findRegionAndCountryByCoordinates(
            request.getLongitude(), 
            request.getLatitude(), 
            request.getCountryName());
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

  @GetMapping("/{regionSlug}")
  public ResponseEntity<Region> getRegionBySlug(@PathVariable String regionSlug) {
    Region region = regionService.getRegionBySlug(regionSlug);
    return ResponseEntity.ok(region);
  }
}
