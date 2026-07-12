package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfboardDTO;
import com.lovettj.surfspotsapi.dto.SurfboardMediaDTO;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.requests.CreateSurfboardMediaRequest;
import com.lovettj.surfspotsapi.requests.CreateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UpdateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UploadSurfboardMediaRequest;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.SurfboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/surfboards")
public class SurfboardController {

    private final SurfboardService surfboardService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SurfboardController(
            SurfboardService surfboardService,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.surfboardService = surfboardService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    @ApiFailureMessage(action = "load", target = "surfboards")
    public ResponseEntity<ApiResponse<List<SurfboardDTO>>> getUserSurfboards(@RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        List<SurfboardDTO> surfboards = surfboardService.getUserSurfboards(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(surfboards));
    }

    @PostMapping
    @ApiFailureMessage(action = "create", target = "surfboard")
    public ResponseEntity<ApiResponse<SurfboardDTO>> createSurfboard(
            @RequestBody CreateSurfboardRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        SurfboardDTO surfboard = surfboardService.createSurfboard(currentUserId, request);
        URI location = CreatedResourceLocations.fromApiPath("/api/surfboards/{surfboardId}", currentUserId, surfboard.getId());
        return ResponseEntity.created(location)
                .body(ApiResponse.success(surfboard, "Surfboard created successfully", HttpStatus.CREATED.value()));
    }

    @GetMapping("/{surfboardId}")
    @ApiFailureMessage(action = "load", target = "surfboard")
    public ResponseEntity<ApiResponse<SurfboardDTO>> getSurfboard(
            @PathVariable String surfboardId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        SurfboardDTO surfboard = surfboardService.getSurfboard(currentUserId, surfboardId);
        return ResponseEntity.ok(ApiResponse.success(surfboard));
    }

    @PutMapping("/{surfboardId}")
    @ApiFailureMessage(action = "update", target = "surfboard")
    public ResponseEntity<ApiResponse<SurfboardDTO>> updateSurfboard(
            @PathVariable String surfboardId,
            @RequestBody UpdateSurfboardRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        SurfboardDTO surfboard = surfboardService.updateSurfboard(currentUserId, surfboardId, request);
        return ResponseEntity.ok(ApiResponse.success(surfboard, "Surfboard updated successfully"));
    }

    @DeleteMapping("/{surfboardId}")
    @ApiFailureMessage(action = "delete", target = "surfboard")
    public ResponseEntity<ApiResponse<String>> deleteSurfboard(
            @PathVariable String surfboardId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        surfboardService.deleteSurfboard(currentUserId, surfboardId);
        return ResponseEntity.ok(ApiResponse.success("Surfboard deleted successfully"));
    }

    @PostMapping("/{surfboardId}/media/upload-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUploadUrl(
            @PathVariable String surfboardId,
            @RequestBody UploadSurfboardMediaRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaUploadUrlResponseHandler.buildUploadUrlResponse(
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
    @ApiFailureMessage(action = "add", target = "surfboard media")
    public ResponseEntity<ApiResponse<SurfboardMediaDTO>> addMedia(
            @PathVariable String surfboardId,
            @RequestBody CreateSurfboardMediaRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.addMediaCreated(
                () -> surfboardService.addMedia(currentUserId, surfboardId, request),
                media -> CreatedResourceLocations.fromApiPath(
                        "/api/surfboards/{surfboardId}/media/{mediaId}", currentUserId, surfboardId, media.getId())
        );
    }

    @DeleteMapping("/media/{mediaId}")
    @ApiFailureMessage(action = "delete", target = "surfboard media")
    public ResponseEntity<ApiResponse<String>> deleteMedia(
            @PathVariable String mediaId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.deleteMedia(() -> surfboardService.deleteMedia(currentUserId, mediaId));
    }
}
