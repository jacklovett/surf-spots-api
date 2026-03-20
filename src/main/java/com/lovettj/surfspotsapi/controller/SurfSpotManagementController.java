package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
  public ResponseEntity<ApiResponse<SurfSpotDTO>> createSurfSpot(@Valid @RequestBody SurfSpotRequest surfSpotRequest) {
    try {
      SurfSpot surfSpot = surfSpotService.createSurfSpot(surfSpotRequest);
      SurfSpotDTO dto = surfSpotService.mapToSurfSpotDTO(surfSpot, surfSpotRequest.getUserId());
      URI location = CreatedResourceLocations.fromApiPath(
          "/api/surf-spots/id/{id}",
          surfSpotRequest.getUserId(),
          dto.getId());
      return ResponseEntity
          .created(location)
          .body(ApiResponse.success(dto, "Surf spot created successfully", HttpStatus.CREATED.value()));
    } catch (ResponseStatusException e) {
      return ResponseEntity.status(e.getStatusCode())
          .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error(ApiErrors.formatErrorMessage("create", "surf spot"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<SurfSpotDTO>> updateSurfSpot(@PathVariable Long id, @Valid @RequestBody SurfSpotRequest surfSpotRequest) {
    try {
      SurfSpot updatedSurfSpot = surfSpotService.updateSurfSpot(id, surfSpotRequest);
      SurfSpotDTO dto = surfSpotService.mapToSurfSpotDTO(updatedSurfSpot, surfSpotRequest.getUserId());
      return ResponseEntity.ok(ApiResponse.success(dto, "Surf spot updated successfully"));
    } catch (ResponseStatusException e) {
      return ResponseEntity.status(e.getStatusCode())
          .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error(ApiErrors.formatErrorMessage("update", "surf spot"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<String>> deleteSurfSpot(
      @PathVariable Long id,
      @RequestParam String userId) {
    try {
      surfSpotService.deleteSurfSpot(id, userId);
      return ResponseEntity.ok(ApiResponse.success("Surf spot deleted successfully"));
    } catch (ResponseStatusException e) {
      return ResponseEntity.status(e.getStatusCode())
          .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error(ApiErrors.formatErrorMessage("delete", "surf spot"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
  }
}

