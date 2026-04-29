package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSessionMediaDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.dto.UserSurfSessionsDTO;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.requests.CreateSurfSessionMediaRequest;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;
import com.lovettj.surfspotsapi.requests.UploadMediaRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.SurfSessionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SurfSessionController {

    private static final Logger logger = LoggerFactory.getLogger(SurfSessionController.class);

    private final SurfSessionService surfSessionService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SurfSessionController(
            SurfSessionService surfSessionService,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.surfSessionService = surfSessionService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/surf-sessions")
    public ResponseEntity<ApiResponse<String>> createSession(@Valid @RequestBody SurfSessionRequest request) {
        try {
            request.setUserId(authenticatedUserResolver.requireCurrentUserId());
            surfSessionService.createSession(request);
            return ResponseEntity.ok(ApiResponse.success("Surf session saved"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ApiErrors.formatErrorMessage("create", "surf session"),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/surf-sessions")
    public ResponseEntity<ApiResponse<UserSurfSessionsDTO>> getSessionsForUser() {
        try {
            String userId = authenticatedUserResolver.requireCurrentUserId();
            UserSurfSessionsDTO payload = surfSessionService.getSurfSessionsForUser(userId);
            return ResponseEntity.ok(ApiResponse.success(payload));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ApiErrors.formatErrorMessage("load", "surf sessions"),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/surf-spots/{id}/sessions")
    public ResponseEntity<SurfSessionSummaryDTO> getSpotSessionsSummary(@PathVariable Long id) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        return ResponseEntity.ok(surfSessionService.getSpotSummaryForUser(id, userId));
    }

    @PostMapping("/surf-sessions/{sessionId}/media/upload-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getMediaUploadUrl(
            @PathVariable Long sessionId,
            @RequestBody UploadMediaRequest request) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        return MediaUploadUrlResponseHandler.buildUploadUrlResponse(
                logger,
                "sessionId",
                String.valueOf(sessionId),
                generatedMediaId -> surfSessionService.getUploadUrl(
                        userId,
                        sessionId,
                        request.getMediaType(),
                        generatedMediaId
                )
        );
    }

    @PostMapping("/surf-sessions/{sessionId}/media")
    public ResponseEntity<ApiResponse<SurfSessionMediaDTO>> addMedia(
            @PathVariable Long sessionId,
            @RequestBody CreateSurfSessionMediaRequest request) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.addMediaCreated(
                logger,
                "sessionId",
                sessionId,
                "surf session media",
                () -> surfSessionService.addMedia(userId, sessionId, request),
                media -> CreatedResourceLocations.fromApiPath(
                        "/api/surf-sessions/{sessionId}/media/{mediaId}", userId, sessionId, media.getId())
        );
    }

    @DeleteMapping("/surf-sessions/media/{mediaId}")
    public ResponseEntity<ApiResponse<String>> deleteMedia(@PathVariable String mediaId) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.deleteMedia(
                "surf session media",
                () -> surfSessionService.deleteMedia(userId, mediaId)
        );
    }
}
