package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.BoundingBox;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/surf-spots")
public class SurfSpotController {
  private final SurfSpotService surfSpotService;

  public SurfSpotController(SurfSpotService surfSpotService) {
    this.surfSpotService = surfSpotService;
  }

  @GetMapping
  public ResponseEntity<List<SurfSpot>> getAllSurfSpots() {
    List<SurfSpot> surfSpots = surfSpotService.getAllSurfSpots();
    return new ResponseEntity<>(surfSpots, HttpStatus.OK);
  }

  @GetMapping("/region/{regionSlug}")
  public ResponseEntity<List<SurfSpotDTO>> getSurfSpotsByRegion(@PathVariable String regionSlug) {
    List<SurfSpotDTO> surfSpots = surfSpotService.findSurfSpotsByRegionSlug(regionSlug);
    return ResponseEntity.ok(surfSpots);
  }

  @GetMapping("/{slug}")
  public ResponseEntity<SurfSpotDTO> getSurfSpotBySlug(
          @PathVariable String slug,
          @RequestParam(required = false) Long userId) {
      return surfSpotService.findBySlugAndUserId(slug, userId)
          .map(ResponseEntity::ok)
          .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/id/{id}")
  public ResponseEntity<SurfSpot> getSurfSpotById(@PathVariable Long id) {
    Optional<SurfSpot> surfSpot = surfSpotService.getSurfSpotById(id);
    return surfSpot.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping("/within-bounds")
  public List<SurfSpotDTO> getSurfSpotsWithinBounds(@RequestBody BoundingBox boundingBox, @RequestParam(required = false) Long userId) {
    return surfSpotService.findSurfSpotsWithinBounds(boundingBox, userId);
  }

  @PostMapping
  public SurfSpot createSurfSpot(@RequestBody SurfSpot surfSpot) {
    return surfSpotService.createSurfSpot(surfSpot);
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
