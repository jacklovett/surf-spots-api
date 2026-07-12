package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.LinkSessionsToSpotResultDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionListItemDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionMediaDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.dto.UserSurfSessionsDTO;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.requests.CreateSurfSessionMediaRequest;
import com.lovettj.surfspotsapi.requests.EndLiveSurfSessionRequest;
import com.lovettj.surfspotsapi.requests.LinkSessionsToSpotRequest;
import com.lovettj.surfspotsapi.requests.StartLiveSurfSessionRequest;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;
import com.lovettj.surfspotsapi.requests.UploadMediaRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.SurfSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SurfSessionController {

    private final SurfSessionService surfSessionService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SurfSessionController(
            SurfSessionService surfSessionService,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.surfSessionService = surfSessionService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/surf-sessions/start")
    @ApiFailureMessage(action = "start", target = "surf session")
    public ResponseEntity<ApiResponse<SurfSessionListItemDTO>> startLiveSession(
            @Valid @RequestBody StartLiveSurfSessionRequest request) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        SurfSessionListItemDTO payload = surfSessionService.startLiveSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(payload));
    }

    @GetMapping("/surf-sessions/in-progress")
    @ApiFailureMessage(action = "load", target = "surf session")
    public ResponseEntity<ApiResponse<SurfSessionListItemDTO>> getInProgressSession() {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        SurfSessionListItemDTO payload = surfSessionService.getInProgressSessionForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @PostMapping("/surf-sessions/{sessionId}/end")
    @ApiFailureMessage(action = "end", target = "surf session")
    public ResponseEntity<ApiResponse<SurfSessionListItemDTO>> endLiveSession(
            @PathVariable Long sessionId, @Valid @RequestBody EndLiveSurfSessionRequest request) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        SurfSessionListItemDTO payload = surfSessionService.endLiveSession(userId, sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @PostMapping("/surf-sessions/link-to-spot")
    @ApiFailureMessage(action = "link", target = "surf sessions")
    public ResponseEntity<ApiResponse<LinkSessionsToSpotResultDTO>> linkSessionsToSpot(
            @Valid @RequestBody LinkSessionsToSpotRequest request) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        LinkSessionsToSpotResultDTO payload = surfSessionService.linkSessionsToSpot(userId, request);
        String message = ApiErrors.linkSessionsToSpotSuccessMessage(payload.getLinkedSessionCount());
        if (message == null) {
            return ResponseEntity.ok(ApiResponse.success(payload));
        }
        return ResponseEntity.ok(ApiResponse.success(payload, message));
    }

    @PostMapping("/surf-sessions")
    @ApiFailureMessage(action = "create", target = "surf session")
    public ResponseEntity<ApiResponse<String>> createSession(@Valid @RequestBody SurfSessionRequest request) {
        request.setUserId(authenticatedUserResolver.requireCurrentUserId());
        surfSessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Surf session saved", "Surf session saved", HttpStatus.CREATED.value()));
    }

    @GetMapping("/surf-sessions")
    @ApiFailureMessage(action = "load", target = "surf sessions")
    public ResponseEntity<ApiResponse<UserSurfSessionsDTO>> getSessionsForUser() {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        UserSurfSessionsDTO payload = surfSessionService.getSurfSessionsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @GetMapping("/surf-sessions/{sessionId}")
    @ApiFailureMessage(action = "load", target = "surf session")
    public ResponseEntity<ApiResponse<SurfSessionListItemDTO>> getSession(@PathVariable Long sessionId) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        SurfSessionListItemDTO payload = surfSessionService.getSessionByIdForUser(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @PutMapping("/surf-sessions/{sessionId}")
    @ApiFailureMessage(action = "update", target = "surf session")
    public ResponseEntity<ApiResponse<String>> updateSession(
            @PathVariable Long sessionId, @Valid @RequestBody SurfSessionRequest request) {
        request.setUserId(authenticatedUserResolver.requireCurrentUserId());
        surfSessionService.updateSession(request.getUserId(), sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("Surf session updated", "Surf session updated", HttpStatus.OK.value()));
    }

    @DeleteMapping("/surf-sessions/{sessionId}")
    @ApiFailureMessage(action = "delete", target = "surf session")
    public ResponseEntity<ApiResponse<String>> deleteSession(@PathVariable Long sessionId) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        surfSessionService.deleteSession(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Surf session deleted", "Surf session deleted", HttpStatus.OK.value()));
    }

    @GetMapping("/surf-spots/{id}/sessions")
    @ApiFailureMessage(action = "load", target = "surf session summary")
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
    @ApiFailureMessage(action = "add", target = "surf session media")
    public ResponseEntity<ApiResponse<SurfSessionMediaDTO>> addMedia(
            @PathVariable Long sessionId,
            @RequestBody CreateSurfSessionMediaRequest request) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.addMediaCreated(
                () -> surfSessionService.addMedia(userId, sessionId, request),
                media -> CreatedResourceLocations.fromApiPath(
                        "/api/surf-sessions/{sessionId}/media/{mediaId}", userId, sessionId, media.getId())
        );
    }

    @DeleteMapping("/surf-sessions/media/{mediaId}")
    @ApiFailureMessage(action = "delete", target = "surf session media")
    public ResponseEntity<ApiResponse<String>> deleteMedia(@PathVariable String mediaId) {
        String userId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.deleteMedia(() -> surfSessionService.deleteMedia(userId, mediaId));
    }
}
