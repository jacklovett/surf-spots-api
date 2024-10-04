package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.entity.SurfSpot;
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
  public ResponseEntity<List<SurfSpot>> getSurfSpotsByRegion(@PathVariable String regionSlug) {
    List<SurfSpot> surfSpots = surfSpotService.findSurfSpotsByRegionSlug(regionSlug);
    return ResponseEntity.ok(surfSpots);
  }

  @GetMapping("/{id}")
  public ResponseEntity<SurfSpot> getSurfSpotById(@PathVariable Long id) {
    Optional<SurfSpot> surfSpot = surfSpotService.getSurfSpotById(id);
    return surfSpot.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/{slug}")
  public ResponseEntity<SurfSpot> getSurfSpotById(@PathVariable String slug) {
    Optional<SurfSpot> surfSpot = surfSpotService.findBySlug(slug);
    return surfSpot.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
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
