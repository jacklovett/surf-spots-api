package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

/**
 * Controller for surf spot write operations (create, update, delete) that require authentication
 */
@RestController
@RequestMapping("/api/surf-spots/management")
public class SurfSpotManagementController {

  private static final Logger log = LoggerFactory.getLogger(SurfSpotManagementController.class);

  private final SurfSpotService surfSpotService;
  private final AuthenticatedUserResolver authenticatedUserResolver;

  public SurfSpotManagementController(
      SurfSpotService surfSpotService,
      AuthenticatedUserResolver authenticatedUserResolver) {
    this.surfSpotService = surfSpotService;
    this.authenticatedUserResolver = authenticatedUserResolver;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<SurfSpotDTO>> createSurfSpot(@Valid @RequestBody SurfSpotRequest surfSpotRequest) {
    try {
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
    } catch (ResponseStatusException e) {
      return ResponseEntity.status(e.getStatusCode())
          .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
    } catch (Exception e) {
      ConstraintViolationException cve = findConstraintViolation(e);
      if (cve != null) {
        log.warn("Create surf spot validation failed: {}", firstConstraintMessage(cve));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(firstConstraintMessage(cve), HttpStatus.BAD_REQUEST.value()));
      }
      log.error("Create surf spot failed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error(ApiErrors.formatErrorMessage("create", "surf spot"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<SurfSpotDTO>> updateSurfSpot(@PathVariable Long id, @Valid @RequestBody SurfSpotRequest surfSpotRequest) {
    try {
      String currentUserId = authenticatedUserResolver.requireCurrentUserId();
      surfSpotRequest.setUserId(currentUserId);
      SurfSpot updatedSurfSpot = surfSpotService.updateSurfSpot(id, surfSpotRequest);
      SurfSpotDTO dto = surfSpotService.mapToSurfSpotDTO(updatedSurfSpot, currentUserId);
      return ResponseEntity.ok(ApiResponse.success(dto, "Surf spot updated successfully"));
    } catch (ResponseStatusException e) {
      return ResponseEntity.status(e.getStatusCode())
          .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
    } catch (Exception e) {
      ConstraintViolationException cve = findConstraintViolation(e);
      if (cve != null) {
        log.warn("Update surf spot validation failed, id={}: {}", id, firstConstraintMessage(cve));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(firstConstraintMessage(cve), HttpStatus.BAD_REQUEST.value()));
      }
      log.error("Update surf spot failed, id={}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error(ApiErrors.formatErrorMessage("update", "surf spot"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<String>> deleteSurfSpot(@PathVariable Long id) {
    try {
      String currentUserId = authenticatedUserResolver.requireCurrentUserId();
      surfSpotService.deleteSurfSpot(id, currentUserId);
      return ResponseEntity.ok(ApiResponse.success("Surf spot deleted successfully"));
    } catch (ResponseStatusException e) {
      return ResponseEntity.status(e.getStatusCode())
          .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
    } catch (Exception e) {
      ConstraintViolationException cve = findConstraintViolation(e);
      if (cve != null) {
        log.warn("Delete surf spot validation failed, id={}: {}", id, firstConstraintMessage(cve));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(firstConstraintMessage(cve), HttpStatus.BAD_REQUEST.value()));
      }
      log.error("Delete surf spot failed, id={}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error(ApiErrors.formatErrorMessage("delete", "surf spot"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
  }

  private static ConstraintViolationException findConstraintViolation(Throwable t) {
    while (t != null) {
      if (t instanceof ConstraintViolationException) {
        return (ConstraintViolationException) t;
      }
      t = t.getCause();
    }
    return null;
  }

  private static String firstConstraintMessage(ConstraintViolationException e) {
    return e.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .findFirst()
        .orElse("Validation failed");
  }
}

