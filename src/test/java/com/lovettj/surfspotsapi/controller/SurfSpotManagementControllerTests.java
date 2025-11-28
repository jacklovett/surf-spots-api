package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.SurfSpotType;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SurfSpotManagementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SurfSpotService surfSpotService;

    private Cookie createValidSessionCookie() {
        // SessionCookieFilter expects a cookie with exactly two parts (payload.signature)
        // Format: "payload.signature" - when split by ".", should have exactly 2 parts
        Cookie sessionCookie = new Cookie("session", "testpayload.testsignature");
        return sessionCookie;
    }

    @Test
    void testCreateSurfSpotShouldReturnCreatedSurfSpot() throws Exception {
        SurfSpot surfSpot = SurfSpot.builder()
                .id(1L)
                .name("Pipeline")
                .description("A famous surf spot.")
                .type(SurfSpotType.REEF_BREAK)
                .longitude(0.1)
                .latitude(0.2)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        Mockito.when(surfSpotService.createSurfSpot(Mockito.any(SurfSpotRequest.class))).thenReturn(surfSpot);

        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Pipeline",
              "description": "A famous surf spot.",
              "type": "Reef Break"
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/surf-spots/id/1"));
    }

    @Test
    void testUpdateSurfSpotShouldReturnUpdatedSurfSpot() throws Exception {
        SurfSpot updatedSurfSpot = SurfSpot.builder()
                .id(1L)
                .name("Updated Pipeline")
                .description("An updated famous surf spot.")
                .type(SurfSpotType.REEF_BREAK)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        SurfSpotDTO updatedDTO = SurfSpotDTO.builder()
                .id(1L)
                .name("Updated Pipeline")
                .description("An updated famous surf spot.")
                .type(SurfSpotType.REEF_BREAK)
                .build();

        Mockito.when(surfSpotService.updateSurfSpot(Mockito.eq(1L), Mockito.any(SurfSpotRequest.class)))
                .thenReturn(updatedSurfSpot);
        Mockito.when(surfSpotService.mapToSurfSpotDTO(Mockito.any(SurfSpot.class), Mockito.anyString()))
                .thenReturn(updatedDTO);

        mockMvc.perform(patch("/api/surf-spots/management/1")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Updated Pipeline",
              "description": "An updated famous surf spot.",
              "type": "Reef Break",
              "userId": "test-user-id-123"
            }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Pipeline")));
    }

    @Test
    void testDeleteSurfSpotShouldReturnNoContent() throws Exception {
        Mockito.doNothing().when(surfSpotService).deleteSurfSpot(1L);

        mockMvc.perform(delete("/api/surf-spots/management/1")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .param("userId", "test-user-id-123"))
                .andExpect(status().isNoContent());

        Mockito.verify(surfSpotService, Mockito.times(1)).deleteSurfSpot(1L);
    }
}

