package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfboardDTO;
import com.lovettj.surfspotsapi.dto.SurfboardImageDTO;
import com.lovettj.surfspotsapi.requests.CreateSurfboardImageRequest;
import com.lovettj.surfspotsapi.requests.CreateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UpdateSurfboardRequest;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.SurfboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/surfboards")
public class SurfboardController {

    private final SurfboardService surfboardService;

    public SurfboardController(SurfboardService surfboardService) {
        this.surfboardService = surfboardService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SurfboardDTO>>> getUserSurfboards(@RequestParam String userId) {
        try {
            List<SurfboardDTO> surfboards = surfboardService.getUserSurfboards(userId);
            return ResponseEntity.ok(ApiResponse.success(surfboards));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SurfboardDTO>> createSurfboard(
            @RequestBody CreateSurfboardRequest request,
            @RequestParam String userId) {
        try {
            SurfboardDTO surfboard = surfboardService.createSurfboard(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(surfboard, "Surfboard created successfully", HttpStatus.CREATED.value()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/{surfboardId}")
    public ResponseEntity<ApiResponse<SurfboardDTO>> getSurfboard(
            @PathVariable String surfboardId,
            @RequestParam String userId) {
        try {
            SurfboardDTO surfboard = surfboardService.getSurfboard(userId, surfboardId);
            return ResponseEntity.ok(ApiResponse.success(surfboard));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/{surfboardId}")
    public ResponseEntity<ApiResponse<SurfboardDTO>> updateSurfboard(
            @PathVariable String surfboardId,
            @RequestBody UpdateSurfboardRequest request,
            @RequestParam String userId) {
        try {
            SurfboardDTO surfboard = surfboardService.updateSurfboard(userId, surfboardId, request);
            return ResponseEntity.ok(ApiResponse.success(surfboard, "Surfboard updated successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{surfboardId}")
    public ResponseEntity<ApiResponse<String>> deleteSurfboard(
            @PathVariable String surfboardId,
            @RequestParam String userId) {
        try {
            surfboardService.deleteSurfboard(userId, surfboardId);
            return ResponseEntity.ok(ApiResponse.success("Surfboard deleted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{surfboardId}/images")
    public ResponseEntity<ApiResponse<SurfboardImageDTO>> addImage(
            @PathVariable String surfboardId,
            @RequestBody CreateSurfboardImageRequest request,
            @RequestParam String userId) {
        try {
            SurfboardImageDTO image = surfboardService.addImage(userId, surfboardId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(image, "Image added successfully", HttpStatus.CREATED.value()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<String>> deleteImage(
            @PathVariable String imageId,
            @RequestParam String userId) {
        try {
            surfboardService.deleteImage(userId, imageId);
            return ResponseEntity.ok(ApiResponse.success("Image deleted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}



