package com.lovettj.surfspotsapi.controller;

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

  @GetMapping("/{regionSlug}")
  public ResponseEntity<Region> getRegionBySlug(@PathVariable String regionSlug) {
    Region region = regionService.getRegionBySlug(regionSlug);
    return ResponseEntity.ok(region);
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
}
