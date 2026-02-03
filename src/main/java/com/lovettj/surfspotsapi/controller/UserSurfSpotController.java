package com.lovettj.surfspotsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lovettj.surfspotsapi.requests.UserSurfSpotRequest;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.UserSurfSpotService;

import com.lovettj.surfspotsapi.dto.UserSurfSpotsDTO;

@RestController
@RequestMapping("/api/user-spots")
public class UserSurfSpotController {

    private final UserSurfSpotService userSurfSpotService;

    public UserSurfSpotController(UserSurfSpotService userSurfSpotService) {
        this.userSurfSpotService = userSurfSpotService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserSurfSpotsDTO> getUserSurfSpotsSummary(@PathVariable String userId) {
        return ResponseEntity.ok(userSurfSpotService.getUserSurfSpotsSummary(userId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> addUserSurfSpot(@RequestBody UserSurfSpotRequest request) {
        try {
            userSurfSpotService.addUserSurfSpot(request.getUserId(), request.getSurfSpotId());
            return ResponseEntity.ok(ApiResponse.success("Surf spot added to user's list."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(e.getMessage(), 500));
        }
    }

    @DeleteMapping("/{userId}/remove/{spotId}")
    public ResponseEntity<ApiResponse<String>> removeUserSurfSpot(@PathVariable String userId, @PathVariable Long spotId) {
        userSurfSpotService.removeUserSurfSpot(userId, spotId);
        return ResponseEntity.ok(ApiResponse.success("Surf spot removed from user's list."));
    }

    @PostMapping("/{userId}/toggle-favourite/{spotId}")
    public ResponseEntity<String> toggleFavourite(@PathVariable String userId, @PathVariable Long spotId) {
        userSurfSpotService.toggleIsFavourite(userId, spotId);
        return ResponseEntity.ok("Surf spot favourite status toggled.");
    }
}
