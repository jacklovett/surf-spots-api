package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import jakarta.validation.Valid;

/**
 * Controller for surf spot write operations (create, update, delete) that require authentication
 */
@RestController
@RequestMapping("/api/surf-spots/management")
public class SurfSpotManagementController {
  private final SurfSpotService surfSpotService;

  public SurfSpotManagementController(SurfSpotService surfSpotService) {
    this.surfSpotService = surfSpotService;
  }

  @PostMapping
  public ResponseEntity<Void> createSurfSpot(@Valid @RequestBody SurfSpotRequest surfSpotRequest) {
      SurfSpot surfSpot = surfSpotService.createSurfSpot(surfSpotRequest);
      URI location = URI.create("/api/surf-spots/id/" + surfSpot.getId());
      return ResponseEntity.created(location).build();
  }

  @PatchMapping("/{id}")
  public ResponseEntity<SurfSpotDTO> updateSurfSpot(@PathVariable Long id, @Valid @RequestBody SurfSpotRequest surfSpotRequest) {
    try {
      SurfSpot updatedSurfSpot = surfSpotService.updateSurfSpot(id, surfSpotRequest);
      SurfSpotDTO dto = surfSpotService.mapToSurfSpotDTO(updatedSurfSpot, surfSpotRequest.getUserId());
      return ResponseEntity.ok(dto);
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

