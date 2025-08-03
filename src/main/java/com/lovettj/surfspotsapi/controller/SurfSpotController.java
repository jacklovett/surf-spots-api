package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.BoundingBox;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.net.URI;

import jakarta.validation.Valid;

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
  public ResponseEntity<List<SurfSpotDTO>> getSurfSpotsByRegion(@PathVariable String regionSlug,
          @RequestParam(required = false) String userId) {
      List<SurfSpotDTO> surfSpots = surfSpotService.findSurfSpotsByRegionSlug(regionSlug, userId);
  
      if (surfSpots.isEmpty()) {
          return ResponseEntity.notFound().build();
      }
  
      return ResponseEntity.ok(surfSpots);
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

  @GetMapping("/within-bounds")
  public List<SurfSpotDTO> getSurfSpotsWithinBounds(
          @RequestParam Double minLatitude,
          @RequestParam Double maxLatitude,
          @RequestParam Double minLongitude,
          @RequestParam Double maxLongitude,
          @RequestParam(required = false) String userId) {
      BoundingBox boundingBox = new BoundingBox(minLatitude, maxLatitude, minLongitude, maxLongitude);
      return surfSpotService.findSurfSpotsWithinBounds(boundingBox, userId);
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
