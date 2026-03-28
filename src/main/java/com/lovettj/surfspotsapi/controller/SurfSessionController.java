package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.SurfSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SurfSessionController {
    private final SurfSessionService surfSessionService;

    public SurfSessionController(SurfSessionService surfSessionService) {
        this.surfSessionService = surfSessionService;
    }

    @PostMapping("/surf-sessions")
    public ResponseEntity<ApiResponse<String>> createSession(@Valid @RequestBody SurfSessionRequest request) {
        surfSessionService.createSession(request);
        return ResponseEntity.ok(ApiResponse.success("Surf session saved"));
    }

    @GetMapping("/surf-spots/{id}/sessions")
    public ResponseEntity<SurfSessionSummaryDTO> getSpotSessionsSummary(
            @PathVariable Long id,
            @RequestParam String userId
    ) {
        return ResponseEntity.ok(surfSessionService.getSpotSummaryForUser(id, userId));
    }
}
