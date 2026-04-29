package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.requests.UserSurfSpotRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.security.AuthenticatedUserResolver;
import com.lovettj.surfspotsapi.service.UserSurfSpotService;

import com.lovettj.surfspotsapi.dto.UserSurfSpotsDTO;

@RestController
@RequestMapping("/api/user-spots")
public class UserSurfSpotController {

    private final UserSurfSpotService userSurfSpotService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public UserSurfSpotController(
            UserSurfSpotService userSurfSpotService,
            AuthenticatedUserResolver authenticatedUserResolver) {
        this.userSurfSpotService = userSurfSpotService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping
    public ResponseEntity<UserSurfSpotsDTO> getUserSurfSpotsSummary() {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return ResponseEntity.ok(userSurfSpotService.getUserSurfSpotsSummary(currentUserId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> addUserSurfSpot(@RequestBody UserSurfSpotRequest request) {
        try {
            request.setUserId(authenticatedUserResolver.requireCurrentUserId());
            userSurfSpotService.addUserSurfSpot(request.getUserId(), request.getSurfSpotId());
            return ResponseEntity.ok(ApiResponse.success("Surf spot added to user's list."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiErrors.formatErrorMessage("add", "surf spot"), 500));
        }
    }

    @DeleteMapping("/remove/{spotId}")
    public ResponseEntity<ApiResponse<String>> removeUserSurfSpot(@PathVariable Long spotId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        userSurfSpotService.removeUserSurfSpot(currentUserId, spotId);
        return ResponseEntity.ok(ApiResponse.success("Surf spot removed from user's list."));
    }

    @PostMapping("/toggle-favourite/{spotId}")
    public ResponseEntity<String> toggleFavourite(@PathVariable Long spotId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        userSurfSpotService.toggleIsFavourite(currentUserId, spotId);
        return ResponseEntity.ok("Surf spot favourite status toggled.");
    }
}
