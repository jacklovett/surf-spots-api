package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSessionListItemDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.SurfSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SurfSessionController {
    private final SurfSessionService surfSessionService;

    public SurfSessionController(SurfSessionService surfSessionService) {
        this.surfSessionService = surfSessionService;
    }

    @PostMapping("/surf-sessions")
    public ResponseEntity<ApiResponse<String>> createSession(@Valid @RequestBody SurfSessionRequest request) {
        try {
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

    @GetMapping("/surf-sessions/mine")
    public ResponseEntity<ApiResponse<List<SurfSessionListItemDTO>>> listMySessions(@RequestParam String userId) {
        try {
            List<SurfSessionListItemDTO> sessions = surfSessionService.listSessionsForUser(userId);
            return ResponseEntity.ok(ApiResponse.success(sessions));
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
    public ResponseEntity<SurfSessionSummaryDTO> getSpotSessionsSummary(
            @PathVariable Long id,
            @RequestParam String userId
    ) {
        return ResponseEntity.ok(surfSessionService.getSpotSummaryForUser(id, userId));
    }
}
