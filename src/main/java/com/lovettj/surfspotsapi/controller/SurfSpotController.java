package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.BoundingBox;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.net.URI;

import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;

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
          if (surfSpots.isEmpty()) {
              return ResponseEntity.notFound().build();
          }
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
          if (surfSpots.isEmpty()) {
              return ResponseEntity.notFound().build();
          }
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
  public ResponseEntity<SurfSpot> getSurfSpotById(@PathVariable Long id) {
    Optional<SurfSpot> surfSpot = surfSpotService.getSurfSpotById(id);
    return surfSpot.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
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

  @PostMapping
  public ResponseEntity<Void> createSurfSpot(@Valid @RequestBody SurfSpotRequest surfSpotRequest) {
      SurfSpot surfSpot = surfSpotService.createSurfSpot(surfSpotRequest);
      URI location = URI.create("/api/surf-spots/" + surfSpot.getId());
      return ResponseEntity.created(location).build();
  }

  @PutMapping("/{id}")
  public ResponseEntity<SurfSpot> updateSurfSpot(@PathVariable Long id, @RequestBody SurfSpot surfSpot) {
    try {
      SurfSpot updatedSurfSpot = surfSpotService.updateSurfSpot(id, surfSpot);
      return ResponseEntity.ok(updatedSurfSpot);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteSurfSpot(@PathVariable Long id) {
    surfSpotService.deleteSurfSpot(id);
    return ResponseEntity.noContent().build();
  }
}
