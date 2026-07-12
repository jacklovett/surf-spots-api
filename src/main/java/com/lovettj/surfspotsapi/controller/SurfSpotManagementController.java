package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import jakarta.validation.Valid;

/**
 * Controller for surf spot write operations (create, update, delete) that require authentication.
 * Errors are mapped to {@link ApiResponse} by {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/surf-spots/management")
public class SurfSpotManagementController {

  private final SurfSpotService surfSpotService;
  private final AuthenticatedUserResolver authenticatedUserResolver;

  public SurfSpotManagementController(
      SurfSpotService surfSpotService,
      AuthenticatedUserResolver authenticatedUserResolver) {
    this.surfSpotService = surfSpotService;
    this.authenticatedUserResolver = authenticatedUserResolver;
  }

  @PostMapping
  @ApiFailureMessage(action = "create", target = "surf spot")
  public ResponseEntity<ApiResponse<SurfSpotDTO>> createSurfSpot(@Valid @RequestBody SurfSpotRequest surfSpotRequest) {
    String currentUserId = authenticatedUserResolver.requireCurrentUserId();
    surfSpotRequest.setUserId(currentUserId);
    SurfSpot surfSpot = surfSpotService.createSurfSpot(surfSpotRequest);
    SurfSpotDTO dto = surfSpotService.mapToSurfSpotDTO(surfSpot, currentUserId);
    URI location = CreatedResourceLocations.fromApiPath(
        "/api/surf-spots/id/{id}",
        currentUserId,
        dto.getId());
    return ResponseEntity
        .created(location)
        .body(ApiResponse.success(dto, "Surf spot created successfully", HttpStatus.CREATED.value()));
  }

  @PatchMapping("/{id}")
  @ApiFailureMessage(action = "update", target = "surf spot")
  public ResponseEntity<ApiResponse<SurfSpotDTO>> updateSurfSpot(
      @PathVariable Long id, @Valid @RequestBody SurfSpotRequest surfSpotRequest) {
    String currentUserId = authenticatedUserResolver.requireCurrentUserId();
    surfSpotRequest.setUserId(currentUserId);
    SurfSpot updatedSurfSpot = surfSpotService.updateSurfSpot(id, surfSpotRequest);
    SurfSpotDTO dto = surfSpotService.mapToSurfSpotDTO(updatedSurfSpot, currentUserId);
    return ResponseEntity.ok(ApiResponse.success(dto, "Surf spot updated successfully"));
  }

  @DeleteMapping("/{id}")
  @ApiFailureMessage(action = "delete", target = "surf spot")
  public ResponseEntity<ApiResponse<String>> deleteSurfSpot(@PathVariable Long id) {
    String currentUserId = authenticatedUserResolver.requireCurrentUserId();
    surfSpotService.deleteSurfSpot(id, currentUserId);
    return ResponseEntity.ok(ApiResponse.success("Surf spot deleted successfully"));
  }
}
