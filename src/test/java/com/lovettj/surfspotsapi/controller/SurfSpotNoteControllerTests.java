package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.dto.SurfSpotNoteDTO;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.requests.SurfSpotNoteRequest;
import com.lovettj.surfspotsapi.response.ApiResponse;
import com.lovettj.surfspotsapi.service.SurfSpotNoteService;

@SpringBootTest
@AutoConfigureMockMvc
class SurfSpotNoteControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SurfSpotNoteService noteService;

    @Autowired
    private ObjectMapper objectMapper;

    private String userId;
    private Long surfSpotId;
    private SurfSpotNoteDTO noteDTO;

    @BeforeEach
    void setUp() {
        userId = "test-user-id";
        surfSpotId = 1L;

        noteDTO = SurfSpotNoteDTO.builder()
                .id(1L)
                .noteText("Test note text")
                .preferredTide(Tide.HIGH)
                .preferredSwellDirection("NW")
                .preferredWind("NW")
                .preferredSwellRange("2-4ft")
                .skillRequirement(SkillLevel.INTERMEDIATE)
                .surfSpotId(surfSpotId)
                .build();
    }

    private Cookie createValidSessionCookie() {
        return new Cookie("session", "testpayload.testsignature");
    }

    @Test
    void testSaveNoteByIdShouldCreateNewNote() throws Exception {
        // Given
        SurfSpotNoteRequest request = new SurfSpotNoteRequest();
        request.setUserId(userId);
        request.setNoteText("New note text");
        request.setPreferredTide(Tide.LOW);

        when(noteService.saveNote(any(SurfSpotNoteRequest.class), eq(surfSpotId)))
                .thenReturn(noteDTO);

        // When & Then
        mockMvc.perform(post("/api/surf-spots/id/{id}/notes", surfSpotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.noteText").value("Test note text"));
    }

    @Test
    void testSaveNoteByIdShouldUpdateExistingNote() throws Exception {
        // Given
        SurfSpotNoteRequest request = new SurfSpotNoteRequest();
        request.setUserId(userId);
        request.setNoteText("Updated note text");
        request.setPreferredTide(Tide.MID);

        SurfSpotNoteDTO updatedNote = SurfSpotNoteDTO.builder()
                .id(1L)
                .noteText("Updated note text")
                .preferredTide(Tide.MID)
                .preferredSwellDirection("SW")
                .preferredWind("SW")
                .preferredSwellRange("3-5ft")
                .skillRequirement(SkillLevel.ADVANCED)
                .surfSpotId(surfSpotId)
                .build();

        when(noteService.saveNote(any(SurfSpotNoteRequest.class), eq(surfSpotId)))
                .thenReturn(updatedNote);

        // When & Then
        mockMvc.perform(post("/api/surf-spots/id/{id}/notes", surfSpotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.noteText").value("Updated note text"))
                .andExpect(jsonPath("$.data.preferredTide").value("Mid"));
    }

    @Test
    void testGetNoteBySurfSpotIdShouldReturnNoteWhenExists() throws Exception {
        // Given
        when(noteService.getNoteForUserAndSpot(userId, surfSpotId))
                .thenReturn(noteDTO);

        // When & Then
        mockMvc.perform(get("/api/surf-spots/id/{id}/notes/{userId}", surfSpotId, userId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.noteText").value("Test note text"))
                .andExpect(jsonPath("$.preferredTide").value("High"));
    }

    @Test
    void testGetNoteBySurfSpotIdShouldReturnNotFoundWhenNoteDoesNotExist() throws Exception {
        // Given
        when(noteService.getNoteForUserAndSpot(userId, surfSpotId))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/surf-spots/id/{id}/notes/{userId}", surfSpotId, userId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isNotFound());
    }
}

