package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfboardDTO;
import com.lovettj.surfspotsapi.dto.SurfboardMediaDTO;
import com.lovettj.surfspotsapi.requests.CreateSurfboardMediaRequest;
import com.lovettj.surfspotsapi.requests.CreateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UpdateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UploadSurfboardMediaRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.SurfboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("create", "surfboard"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
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
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("load", "surfboard"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
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
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("update", "surfboard"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
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
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("delete", "surfboard"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{surfboardId}/media/upload-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUploadUrl(
            @PathVariable String surfboardId,
            @RequestBody UploadSurfboardMediaRequest request,
            @RequestParam String userId) {
        try {
            String mediaId = UUID.randomUUID().toString();
            String uploadUrl = surfboardService.getUploadUrl(userId, surfboardId, request.getMediaType(), mediaId);
            return ResponseEntity.ok(ApiResponse.success(Map.of("uploadUrl", uploadUrl, "mediaId", mediaId)));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(ApiErrors.MEDIA_UPLOAD_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(ApiErrors.MEDIA_UPLOAD_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(ApiErrors.MEDIA_UPLOAD_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value()));
        }
    }

    @PostMapping("/{surfboardId}/media")
    public ResponseEntity<ApiResponse<SurfboardMediaDTO>> addMedia(
            @PathVariable String surfboardId,
            @RequestBody CreateSurfboardMediaRequest request,
            @RequestParam String userId) {
        try {
            SurfboardMediaDTO media = surfboardService.addMedia(userId, surfboardId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(media, "Media added successfully", HttpStatus.CREATED.value()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("add", "surfboard media"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<ApiResponse<String>> deleteMedia(
            @PathVariable String mediaId,
            @RequestParam String userId) {
        try {
            surfboardService.deleteMedia(userId, mediaId);
            return ResponseEntity.ok(ApiResponse.success("Media deleted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("delete", "surfboard media"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}



