package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.TripDTO;
import com.lovettj.surfspotsapi.http.CreatedResourceLocations;
import com.lovettj.surfspotsapi.requests.*;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public TripController(TripService tripService, AuthenticatedUserResolver authenticatedUserResolver) {
        this.tripService = tripService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping
    @ApiFailureMessage(action = "create", target = "trip")
    public ResponseEntity<ApiResponse<TripDTO>> createTrip(
            @RequestBody CreateTripRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        TripDTO trip = tripService.createTrip(currentUserId, request);
        URI location = CreatedResourceLocations.fromApiPath("/api/trips/{tripId}", currentUserId, trip.getId());
        return ResponseEntity.created(location)
                .body(ApiResponse.success(trip, "Trip created successfully", HttpStatus.CREATED.value()));
    }

    @PutMapping("/{tripId}")
    @ApiFailureMessage(action = "update", target = "trip")
    public ResponseEntity<ApiResponse<TripDTO>> updateTrip(
            @PathVariable String tripId,
            @RequestBody UpdateTripRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        TripDTO trip = tripService.updateTrip(currentUserId, tripId, request);
        return ResponseEntity.ok(ApiResponse.success(trip, "Trip updated successfully"));
    }

    @DeleteMapping("/{tripId}")
    @ApiFailureMessage(action = "delete", target = "trip")
    public ResponseEntity<ApiResponse<String>> deleteTrip(
            @PathVariable String tripId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        tripService.deleteTrip(currentUserId, tripId);
        return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully"));
    }

    @GetMapping("/{tripId}")
    @ApiFailureMessage(action = "load", target = "trip")
    public ResponseEntity<ApiResponse<TripDTO>> getTrip(
            @PathVariable String tripId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        TripDTO trip = tripService.getTrip(currentUserId, tripId);
        return ResponseEntity.ok(ApiResponse.success(trip));
    }

    /**
     * Lists trips for a user. Uses {@code /user/{userId}} so this route does not collide with
     * {@link #getTrip(String, String)} ({@code GET /{tripId}}) when both identifiers are UUID-shaped strings.
     */
    @GetMapping("/user/{userId}")
    @ApiFailureMessage(action = "load", target = "trips")
    public ResponseEntity<ApiResponse<List<TripDTO>>> getUserTrips(@PathVariable String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        List<TripDTO> trips = tripService.getUserTrips(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    @PostMapping("/{tripId}/spots/{surfSpotId}")
    @ApiFailureMessage(action = "add spot to", target = "trip")
    public ResponseEntity<ApiResponse<String>> addSpot(
            @PathVariable String tripId,
            @PathVariable Long surfSpotId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        tripService.addSpot(currentUserId, tripId, surfSpotId);
        return ResponseEntity.ok(ApiResponse.success("Spot added to trip"));
    }

    @DeleteMapping("/{tripId}/spots/{tripSpotId}")
    @ApiFailureMessage(action = "remove spot from", target = "trip")
    public ResponseEntity<ApiResponse<String>> removeSpot(
            @PathVariable String tripId,
            @PathVariable String tripSpotId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        tripService.removeSpot(currentUserId, tripId, tripSpotId);
        return ResponseEntity.ok(ApiResponse.success("Spot removed from trip"));
    }

    @PostMapping("/{tripId}/surfboards/{surfboardId}")
    @ApiFailureMessage(action = "add surfboard to", target = "trip")
    public ResponseEntity<ApiResponse<String>> addSurfboard(
            @PathVariable String tripId,
            @PathVariable String surfboardId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        tripService.addSurfboard(currentUserId, tripId, surfboardId);
        return ResponseEntity.ok(ApiResponse.success("Surfboard added to trip"));
    }

    @DeleteMapping("/{tripId}/surfboards/{tripSurfboardId}")
    @ApiFailureMessage(action = "remove surfboard from", target = "trip")
    public ResponseEntity<ApiResponse<String>> removeSurfboard(
            @PathVariable String tripId,
            @PathVariable String tripSurfboardId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        tripService.removeSurfboard(currentUserId, tripId, tripSurfboardId);
        return ResponseEntity.ok(ApiResponse.success("Surfboard removed from trip"));
    }

    @PostMapping("/{tripId}/members")
    @ApiFailureMessage(action = "add", target = "member")
    public ResponseEntity<ApiResponse<String>> addMember(
            @PathVariable String tripId,
            @RequestBody AddTripMemberRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        tripService.addMember(currentUserId, tripId, request);
        return ResponseEntity.ok(ApiResponse.success("Invitation sent"));
    }

    @DeleteMapping("/{tripId}/members/{memberUserId}")
    @ApiFailureMessage(action = "remove", target = "member")
    public ResponseEntity<ApiResponse<String>> removeMember(
            @PathVariable String tripId,
            @PathVariable String memberUserId) {
        String authenticatedUserId = authenticatedUserResolver.requireCurrentUserId();
        tripService.removeMember(authenticatedUserId, tripId, memberUserId);
        return ResponseEntity.ok(ApiResponse.success("Member removed from trip"));
    }

    @DeleteMapping("/{tripId}/invitations/{invitationId}")
    @ApiFailureMessage(action = "cancel", target = "invitation")
    public ResponseEntity<ApiResponse<String>> cancelInvitation(
            @PathVariable String tripId,
            @PathVariable String invitationId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        tripService.cancelInvitation(currentUserId, tripId, invitationId);
        return ResponseEntity.ok(ApiResponse.success("Invitation cancelled"));
    }

    @PostMapping("/{tripId}/media/upload-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUploadUrl(
            @PathVariable String tripId,
            @RequestBody UploadMediaRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaUploadUrlResponseHandler.buildUploadUrlResponse(
                "tripId",
                tripId,
                generatedMediaId -> tripService.getUploadUrl(currentUserId, tripId, request, generatedMediaId)
        );
    }

    @PostMapping("/{tripId}/media")
    @ApiFailureMessage(action = "save", target = "trip media")
    public ResponseEntity<ApiResponse<String>> recordMedia(
            @PathVariable String tripId,
            @RequestBody RecordMediaRequest request,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.recordMediaOk(() -> tripService.recordMedia(currentUserId, tripId, request));
    }

    @DeleteMapping("/{tripId}/media/{mediaId}")
    @ApiFailureMessage(action = "delete", target = "trip media")
    public ResponseEntity<ApiResponse<String>> deleteMedia(
            @PathVariable String tripId,
            @PathVariable String mediaId,
            @RequestParam(required = false) String userId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return MediaMutationResponseHandler.deleteMedia(() -> tripService.deleteMedia(currentUserId, tripId, mediaId));
    }
}
