package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.TripDTO;
import com.lovettj.surfspotsapi.requests.*;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TripDTO>> createTrip(
            @RequestBody CreateTripRequest request,
            @RequestParam String userId) {
        try {
            TripDTO trip = tripService.createTrip(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(trip, "Trip created successfully", HttpStatus.CREATED.value()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripDTO>> updateTrip(
            @PathVariable String tripId,
            @RequestBody UpdateTripRequest request,
            @RequestParam String userId) {
        try {
            TripDTO trip = tripService.updateTrip(userId, tripId, request);
            return ResponseEntity.ok(ApiResponse.success(trip, "Trip updated successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<ApiResponse<String>> deleteTrip(
            @PathVariable String tripId,
            @RequestParam String userId) {
        try {
            tripService.deleteTrip(userId, tripId);
            return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripDTO>> getTrip(
            @PathVariable String tripId,
            @RequestParam String userId) {
        try {
            TripDTO trip = tripService.getTrip(userId, tripId);
            return ResponseEntity.ok(ApiResponse.success(trip));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<TripDTO>>> getUserTrips(@RequestParam String userId) {
        try {
            List<TripDTO> trips = tripService.getUserTrips(userId);
            return ResponseEntity.ok(ApiResponse.success(trips));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{tripId}/spots/{surfSpotId}")
    public ResponseEntity<ApiResponse<String>> addSpot(
            @PathVariable String tripId,
            @PathVariable Long surfSpotId,
            @RequestParam String userId) {
        try {
            tripService.addSpot(userId, tripId, surfSpotId);
            return ResponseEntity.ok(ApiResponse.success("Spot added to trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}/spots/{tripSpotId}")
    public ResponseEntity<ApiResponse<String>> removeSpot(
            @PathVariable String tripId,
            @PathVariable String tripSpotId,
            @RequestParam String userId) {
        try {
            tripService.removeSpot(userId, tripId, tripSpotId);
            return ResponseEntity.ok(ApiResponse.success("Spot removed from trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{tripId}/surfboards/{surfboardId}")
    public ResponseEntity<ApiResponse<String>> addSurfboard(
            @PathVariable String tripId,
            @PathVariable String surfboardId,
            @RequestParam String userId) {
        try {
            tripService.addSurfboard(userId, tripId, surfboardId);
            return ResponseEntity.ok(ApiResponse.success("Surfboard added to trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}/surfboards/{tripSurfboardId}")
    public ResponseEntity<ApiResponse<String>> removeSurfboard(
            @PathVariable String tripId,
            @PathVariable String tripSurfboardId,
            @RequestParam String userId) {
        try {
            tripService.removeSurfboard(userId, tripId, tripSurfboardId);
            return ResponseEntity.ok(ApiResponse.success("Surfboard removed from trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{tripId}/members")
    public ResponseEntity<ApiResponse<String>> addMember(
            @PathVariable String tripId,
            @RequestBody AddTripMemberRequest request,
            @RequestParam String userId) {
        try {
            tripService.addMember(userId, tripId, request);
            return ResponseEntity.ok(ApiResponse.success("Invitation sent"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}/members/{userId}")
    public ResponseEntity<ApiResponse<String>> removeMember(
            @PathVariable String tripId,
            @PathVariable String userId,
            @RequestParam String currentUserId) {
        try {
            tripService.removeMember(currentUserId, tripId, userId);
            return ResponseEntity.ok(ApiResponse.success("Member removed from trip"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}/invitations/{invitationId}")
    public ResponseEntity<ApiResponse<String>> cancelInvitation(
            @PathVariable String tripId,
            @PathVariable String invitationId,
            @RequestParam String userId) {
        try {
            tripService.cancelInvitation(userId, tripId, invitationId);
            return ResponseEntity.ok(ApiResponse.success("Invitation cancelled"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{tripId}/media/upload-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUploadUrl(
            @PathVariable String tripId,
            @RequestBody UploadMediaRequest request,
            @RequestParam String userId) {
        try {
            // Generate media ID first
            String mediaId = UUID.randomUUID().toString();
            String uploadUrl = tripService.getUploadUrl(userId, tripId, request, mediaId);
            return ResponseEntity.ok(ApiResponse.success(Map.of("uploadUrl", uploadUrl, "mediaId", mediaId)));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{tripId}/media")
    public ResponseEntity<ApiResponse<String>> recordMedia(
            @PathVariable String tripId,
            @RequestBody RecordMediaRequest request,
            @RequestParam String userId) {
        try {
            tripService.recordMedia(userId, tripId, request);
            return ResponseEntity.ok(ApiResponse.success("Media recorded successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{tripId}/media/{mediaId}")
    public ResponseEntity<ApiResponse<String>> deleteMedia(
            @PathVariable String tripId,
            @PathVariable String mediaId,
            @RequestParam String userId) {
        try {
            tripService.deleteMedia(userId, tripId, mediaId);
            return ResponseEntity.ok(ApiResponse.success("Media deleted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ApiResponse.error(e.getReason(), e.getStatusCode().value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}

