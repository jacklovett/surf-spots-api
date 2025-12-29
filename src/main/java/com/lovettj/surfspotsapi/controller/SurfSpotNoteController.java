package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotNoteDTO;
import com.lovettj.surfspotsapi.requests.SurfSpotNoteRequest;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.SurfSpotNoteService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class SurfSpotNoteController {
    private final SurfSpotNoteService noteService;

    public SurfSpotNoteController(SurfSpotNoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/surf-spots/id/{id}/notes/{userId}")
    public ResponseEntity<SurfSpotNoteDTO> getNoteBySurfSpotId(
            @PathVariable Long id,
            @PathVariable String userId) {
        SurfSpotNoteDTO note = noteService.getNoteForUserAndSpot(userId, id);
        if (note == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(note);
    }

    @PostMapping("/surf-spots/id/{id}/notes")
    public ResponseEntity<ApiResponse<SurfSpotNoteDTO>> saveNoteById(
            @PathVariable Long id,
            @Valid @RequestBody SurfSpotNoteRequest request) {
        SurfSpotNoteDTO savedNote = noteService.saveNote(request, id);
        return ResponseEntity.ok(ApiResponse.success(savedNote, "Note saved successfully"));
    }

}
