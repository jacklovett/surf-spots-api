package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.requests.UserSurfSpotRequest;
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
    @ApiFailureMessage(action = "load", target = "user surf spots")
    public ResponseEntity<UserSurfSpotsDTO> getUserSurfSpotsSummary() {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        return ResponseEntity.ok(userSurfSpotService.getUserSurfSpotsSummary(currentUserId));
    }

    @PostMapping
    @ApiFailureMessage(action = "add", target = "surf spot")
    public ResponseEntity<ApiResponse<String>> addUserSurfSpot(@RequestBody UserSurfSpotRequest request) {
        request.setUserId(authenticatedUserResolver.requireCurrentUserId());
        userSurfSpotService.addUserSurfSpot(request.getUserId(), request.getSurfSpotId());
        return ResponseEntity.ok(ApiResponse.success("Surf spot added to user's list."));
    }

    @DeleteMapping("/remove/{spotId}")
    @ApiFailureMessage(action = "remove", target = "surf spot")
    public ResponseEntity<ApiResponse<String>> removeUserSurfSpot(@PathVariable Long spotId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        userSurfSpotService.removeUserSurfSpot(currentUserId, spotId);
        return ResponseEntity.ok(ApiResponse.success("Surf spot removed from user's list."));
    }

    @PostMapping("/toggle-favourite/{spotId}")
    @ApiFailureMessage(action = "update", target = "favourite status")
    public ResponseEntity<String> toggleFavourite(@PathVariable Long spotId) {
        String currentUserId = authenticatedUserResolver.requireCurrentUserId();
        userSurfSpotService.toggleIsFavourite(currentUserId, spotId);
        return ResponseEntity.ok("Surf spot favourite status toggled.");
    }
}
