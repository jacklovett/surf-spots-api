package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.service.SubRegionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sub-regions")
public class SubRegionController {
  private final SubRegionService subRegionService;

  public SubRegionController(SubRegionService subRegionService) {
    this.subRegionService = subRegionService;
  }

  @GetMapping("/{subRegionSlug}")
  public ResponseEntity<SubRegion> getSubRegionBySlug(@PathVariable String subRegionSlug) {
    SubRegion subRegion = subRegionService.getSubRegionBySlug(subRegionSlug);
    return ResponseEntity.ok(subRegion);
  }

  @GetMapping("/region/{regionId}")
  public ResponseEntity<List<SubRegion>> getSubRegionsByRegionId(@PathVariable Long regionId) {
    List<SubRegion> subRegions = subRegionService.getSubRegionsByRegion(regionId);
    return ResponseEntity.ok(subRegions);
  }

  @GetMapping("/{regionSlug}/sub-regions")
  public ResponseEntity<List<SubRegion>> getSubRegionsByRegionSlug(@PathVariable String regionSlug) {
    List<SubRegion> subRegions = subRegionService.findSubRegionsByRegionSlug(regionSlug);
    if (subRegions.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(subRegions);
  }
}




