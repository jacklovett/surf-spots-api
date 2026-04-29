package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.TripDTO;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.requests.*;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private static final Logger logger = LoggerFactory.getLogger(TripController.class);

    private final TripService tripService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public TripController(TripService tripService, AuthenticatedUserResolver authenticatedUserResolver) {
        this.tripService = tripService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TripDTO>> createTrip(
            @RequestBody CreateTripRequest request,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            TripDTO trip = tripService.createTrip(currentUserId, request);
            URI location = CreatedResourceLocations.fromApiPath("/api/trips/{tripId}", currentUserId, trip.getId());
            return ResponseEntity.created(location)
                    .body(ApiResponse.success(trip, "Trip created successfully", HttpStatus.CREATED.value()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("create", "trip"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripDTO>> updateTrip(
            @PathVariable String tripId,
            @RequestBody UpdateTripRequest request,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            TripDTO trip = tripService.updateTrip(currentUserId, tripId, request);
            return ResponseEntity.ok(ApiResponse.success(trip, "Trip updated successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("update", "trip"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<ApiResponse<String>> deleteTrip(
            @PathVariable String tripId,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            tripService.deleteTrip(currentUserId, tripId);
            return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("delete", "trip"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripDTO>> getTrip(
            @PathVariable String tripId,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            TripDTO trip = tripService.getTrip(currentUserId, tripId);
            return ResponseEntity.ok(ApiResponse.success(trip));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("load", "trip"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Lists trips for a user. Uses {@code /user/{userId}} so this route does not collide with
     * {@link #getTrip(String, String)} ({@code GET /{tripId}}) when both identifiers are UUID-shaped strings.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TripDTO>>> getUserTrips(@PathVariable String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            List<TripDTO> trips = tripService.getUserTrips(currentUserId);
            return ResponseEntity.ok(ApiResponse.success(trips));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("load", "trips"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{tripId}/spots/{surfSpotId}")
    public ResponseEntity<ApiResponse<String>> addSpot(
            @PathVariable String tripId,
            @PathVariable Long surfSpotId,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            tripService.addSpot(currentUserId, tripId, surfSpotId);
            return ResponseEntity.ok(ApiResponse.success("Spot added to trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("add spot to", "trip"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}/spots/{tripSpotId}")
    public ResponseEntity<ApiResponse<String>> removeSpot(
            @PathVariable String tripId,
            @PathVariable String tripSpotId,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            tripService.removeSpot(currentUserId, tripId, tripSpotId);
            return ResponseEntity.ok(ApiResponse.success("Spot removed from trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("remove spot from", "trip"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{tripId}/surfboards/{surfboardId}")
    public ResponseEntity<ApiResponse<String>> addSurfboard(
            @PathVariable String tripId,
            @PathVariable String surfboardId,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            tripService.addSurfboard(currentUserId, tripId, surfboardId);
            return ResponseEntity.ok(ApiResponse.success("Surfboard added to trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("add surfboard to", "trip"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}/surfboards/{tripSurfboardId}")
    public ResponseEntity<ApiResponse<String>> removeSurfboard(
            @PathVariable String tripId,
            @PathVariable String tripSurfboardId,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            tripService.removeSurfboard(currentUserId, tripId, tripSurfboardId);
            return ResponseEntity.ok(ApiResponse.success("Surfboard removed from trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("remove surfboard from", "trip"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{tripId}/members")
    public ResponseEntity<ApiResponse<String>> addMember(
            @PathVariable String tripId,
            @RequestBody AddTripMemberRequest request,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            tripService.addMember(currentUserId, tripId, request);
            return ResponseEntity.ok(ApiResponse.success("Invitation sent"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("add", "member"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}/members/{memberUserId}")
    public ResponseEntity<ApiResponse<String>> removeMember(
            @PathVariable String tripId,
            @PathVariable String memberUserId) {
        try {
            String authenticatedUserId = authenticatedUserResolver.requireCurrentUserId();
            tripService.removeMember(authenticatedUserId, tripId, memberUserId);
            return ResponseEntity.ok(ApiResponse.success("Member removed from trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("remove", "member"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}/invitations/{invitationId}")
    public ResponseEntity<ApiResponse<String>> cancelInvitation(
            @PathVariable String tripId,
            @PathVariable String invitationId,
            @RequestParam(required = false) String userId) {
        try {
            String currentUserId = authenticatedUserResolver.requireCurrentUserId();
            tripService.cancelInvitation(currentUserId, tripId, invitationId);
            return ResponseEntity.ok(ApiResponse.success("Invitation cancelled"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("cancel", "invitation"), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{tripId}/media/upload-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUploadUrl(
            @PathVariable String tripId,
            @RequestBody UploadMediaRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaUploadUrlResponseHandler.buildUploadUrlResponse(
                logger,
                "tripId",
                tripId,
                generatedMediaId -> tripService.getUploadUrl(currentUserId, tripId, request, generatedMediaId)
        );
    }

    @PostMapping("/{tripId}/media")
    public ResponseEntity<ApiResponse<String>> recordMedia(
            @PathVariable String tripId,
            @RequestBody RecordMediaRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.recordMediaOk(
                logger,
                tripId,
                "trip media",
                () -> tripService.recordMedia(currentUserId, tripId, request)
        );
    }

    @DeleteMapping("/{tripId}/media/{mediaId}")
    public ResponseEntity<ApiResponse<String>> deleteMedia(
            @PathVariable String tripId,
            @PathVariable String mediaId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.deleteMedia(
                "trip media",
                () -> tripService.deleteMedia(currentUserId, tripId, mediaId)
        );
    }
}

