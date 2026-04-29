package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfboardDTO;
import com.lovettj.surfspotsapi.dto.SurfboardMediaDTO;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.requests.CreateSurfboardMediaRequest;
import com.lovettj.surfspotsapi.requests.CreateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UpdateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UploadSurfboardMediaRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.SurfboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/surfboards")
public class SurfboardController {

    private static final Logger logger = LoggerFactory.getLogger(SurfboardController.class);

    private final SurfboardService surfboardService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SurfboardController(
            SurfboardService surfboardService,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.surfboardService = surfboardService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SurfboardDTO>>> getUserSurfboards(@RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            List<SurfboardDTO> surfboards = surfboardService.getUserSurfboards(currentUserId);
            return ResponseEntity.ok(ApiResponse.success(surfboards));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("load", "surfboards"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SurfboardDTO>> createSurfboard(
            @RequestBody CreateSurfboardRequest request,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            SurfboardDTO surfboard = surfboardService.createSurfboard(currentUserId, request);
            URI location = CreatedResourceLocations.fromApiPath("/api/surfboards/{surfboardId}", currentUserId, surfboard.getId());
            return ResponseEntity.created(location)
                    .body(ApiResponse.success(surfboard, "Surfboard created successfully", HttpStatus.CREATED.value()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("create", "surfboard"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/{surfboardId}")
    public ResponseEntity<ApiResponse<SurfboardDTO>> getSurfboard(
            @PathVariable String surfboardId,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            SurfboardDTO surfboard = surfboardService.getSurfboard(currentUserId, surfboardId);
            return ResponseEntity.ok(ApiResponse.success(surfboard));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("load", "surfboard"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/{surfboardId}")
    public ResponseEntity<ApiResponse<SurfboardDTO>> updateSurfboard(
            @PathVariable String surfboardId,
            @RequestBody UpdateSurfboardRequest request,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            SurfboardDTO surfboard = surfboardService.updateSurfboard(currentUserId, surfboardId, request);
            return ResponseEntity.ok(ApiResponse.success(surfboard, "Surfboard updated successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("update", "surfboard"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{surfboardId}")
    public ResponseEntity<ApiResponse<String>> deleteSurfboard(
            @PathVariable String surfboardId,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            surfboardService.deleteSurfboard(currentUserId, surfboardId);
            return ResponseEntity.ok(ApiResponse.success("Surfboard deleted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("delete", "surfboard"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{surfboardId}/media/upload-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUploadUrl(
            @PathVariable String surfboardId,
            @RequestBody UploadSurfboardMediaRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaUploadUrlResponseHandler.buildUploadUrlResponse(
                logger,
                "surfboardId",
                surfboardId,
                generatedMediaId -> surfboardService.getUploadUrl(
                        currentUserId,
                        surfboardId,
                        request.getMediaType(),
                        generatedMediaId
                )
        );
    }

    @PostMapping("/{surfboardId}/media")
    public ResponseEntity<ApiResponse<SurfboardMediaDTO>> addMedia(
            @PathVariable String surfboardId,
            @RequestBody CreateSurfboardMediaRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.addMediaCreated(
                logger,
                "surfboardId",
                surfboardId,
                "surfboard media",
                () -> surfboardService.addMedia(currentUserId, surfboardId, request),
                media -> CreatedResourceLocations.fromApiPath(
                        "/api/surfboards/{surfboardId}/media/{mediaId}", currentUserId, surfboardId, media.getId())
        );
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<ApiResponse<String>> deleteMedia(
            @PathVariable String mediaId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.deleteMedia(
                "surfboard media",
                () -> surfboardService.deleteMedia(currentUserId, mediaId)
        );
    }
}



