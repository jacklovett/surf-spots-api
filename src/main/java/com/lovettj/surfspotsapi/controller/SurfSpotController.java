package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.requests.BoundingBox;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

/**
 * Controller for read-only surf spot endpoints that don't require authentication
 */
@RestController
@RequestMapping("/api/surf-spots")
public class SurfSpotController {
  private final SurfSpotService surfSpotService;

  public SurfSpotController(SurfSpotService surfSpotService) {
    this.surfSpotService = surfSpotService;
  }

  @PostMapping("/region/{regionSlug}")
  public ResponseEntity<List<SurfSpotDTO>> getSurfSpotsByRegionWithFilters(
          @PathVariable String regionSlug,
          @RequestBody SurfSpotFilterDTO filters) {
      try {
          List<SurfSpotDTO> surfSpots = surfSpotService.findSurfSpotsByRegionSlugWithFilters(regionSlug, filters);
          return ResponseEntity.ok(surfSpots);
      } catch (EntityNotFoundException e) {
          return ResponseEntity.status(500).build();
      }
  }

  @PostMapping("/sub-region/{subRegionSlug}")
  public ResponseEntity<List<SurfSpotDTO>> getSurfSpotsBySubRegionWithFilters(
          @PathVariable String subRegionSlug,
          @RequestBody SurfSpotFilterDTO filters) {
      try {
          List<SurfSpotDTO> surfSpots = surfSpotService.findSurfSpotsBySubRegionSlugWithFilters(subRegionSlug, filters);
          return ResponseEntity.ok(surfSpots);
      } catch (EntityNotFoundException e) {
          return ResponseEntity.status(500).build();
      }
  }

  @GetMapping("/{slug}")
  public ResponseEntity<SurfSpotDTO> getSurfSpotBySlug(@PathVariable String slug,
          @RequestParam(required = false) String userId) {
      return surfSpotService.findBySlugAndUserId(slug, userId)
              .map(ResponseEntity::ok)
              .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/id/{id}")
  public ResponseEntity<SurfSpotDTO> getSurfSpotById(@PathVariable Long id,
          @RequestParam(required = false) String userId) {
    return surfSpotService.findByIdAndUserId(id, userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/within-bounds")
  public List<SurfSpotDTO> getSurfSpotsWithinBoundsWithFilters(
          @RequestBody SurfSpotBoundsFilterDTO boundsFilter) {
      BoundingBox boundingBox = new BoundingBox(
          boundsFilter.getMinLatitude(), boundsFilter.getMaxLatitude(),
          boundsFilter.getMinLongitude(), boundsFilter.getMaxLongitude()
      );
      return surfSpotService.findSurfSpotsWithinBoundsWithFilters(boundingBox, boundsFilter);
  }
}
