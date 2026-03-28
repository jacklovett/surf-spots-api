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

import org.springframework.web.server.ResponseStatusException;

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

        SurfSpotDTO createdDTO = SurfSpotDTO.builder()
                .id(1L)
                .name("Pipeline")
                .slug("pipeline")
                .path("/surf-spots/europe/spain/andalusia/pipeline")
                .build();
        Mockito.when(surfSpotService.createSurfSpot(Mockito.any(SurfSpotRequest.class))).thenReturn(surfSpot);
        Mockito.when(surfSpotService.mapToSurfSpotDTO(Mockito.any(SurfSpot.class), Mockito.anyString()))
                .thenReturn(createdDTO);

        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Pipeline",
              "description": "A famous surf spot.",
              "regionId": 1,
              "latitude": 0.2,
              "longitude": 0.1,
              "status": "Pending",
              "type": "Reef Break",
              "beachBottomType": "Rock",
              "skillLevel": "Intermediate",
              "waveDirection": "Left",
              "swellDirection": "N",
              "windDirection": "SW",
              "userId": "test-user-id-123"
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/surf-spots/id/1?userId=test-user-id-123"))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.path", is("/surf-spots/europe/spain/andalusia/pipeline")))
                .andExpect(jsonPath("$.success", is(true)));
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
              "regionId": 1,
              "status": "Private",
              "userId": "test-user-id-123"
            }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Updated Pipeline")))
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testCreateSurfSpotWithForecastsAndWebcamsShouldAcceptPayload() throws Exception {
        SurfSpot surfSpot = SurfSpot.builder()
                .id(1L)
                .name("Spot With Links")
                .description("Spot with forecast and webcam links.")
                .longitude(0.1)
                .latitude(0.2)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        SurfSpotDTO createdDTO = SurfSpotDTO.builder()
                .id(1L)
                .name("Spot With Links")
                .slug("spot-with-links")
                .path("/surf-spots/europe/spain/andalusia/spot-with-links")
                .build();
        Mockito.when(surfSpotService.createSurfSpot(Mockito.any(SurfSpotRequest.class))).thenReturn(surfSpot);
        Mockito.when(surfSpotService.mapToSurfSpotDTO(Mockito.any(SurfSpot.class), Mockito.anyString()))
                .thenReturn(createdDTO);

        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Spot With Links",
              "description": "Spot with forecast and webcam links.",
              "regionId": 1,
              "userId": "test-user-id-123",
              "latitude": 0.2,
              "longitude": 0.1,
              "status": "Pending",
              "type": "Beach Break",
              "beachBottomType": "Sand",
              "skillLevel": "Beginner",
              "waveDirection": "Left",
              "swellDirection": "NW",
              "windDirection": "SE",
              "forecasts": ["https://forecast.example.com/1"],
              "webcams": ["https://webcam.example.com/1"]
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/surf-spots/id/1?userId=test-user-id-123"))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.path", is("/surf-spots/europe/spain/andalusia/spot-with-links")))
                .andExpect(jsonPath("$.success", is(true)));

        Mockito.verify(surfSpotService).createSurfSpot(Mockito.argThat(req ->
                req.getForecasts() != null && req.getForecasts().size() == 1
                        && req.getWebcams() != null && req.getWebcams().size() == 1));
    }

    @Test
    void testCreateSurfSpotWithEmptyTypeStringShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Invalid Type Payload",
              "description": "Empty string is not a valid enum token.",
              "regionId": 1,
              "type": "",
              "userId": "test-user-id-123"
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateSurfSpotWithStandingWaveTypeShouldReturnCreatedSurfSpot() throws Exception {
        SurfSpot surfSpot = SurfSpot.builder()
                .id(1L)
                .name("Eisbach")
                .description("Famous standing river wave in Munich.")
                .type(SurfSpotType.STANDING_WAVE)
                .longitude(11.5884)
                .latitude(48.1522)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        SurfSpotDTO createdDTO = SurfSpotDTO.builder()
                .id(1L)
                .name("Eisbach")
                .slug("eisbach")
                .path("/surf-spots/europe/germany/bavaria/eisbach")
                .build();
        Mockito.when(surfSpotService.createSurfSpot(Mockito.any(SurfSpotRequest.class))).thenReturn(surfSpot);
        Mockito.when(surfSpotService.mapToSurfSpotDTO(Mockito.any(SurfSpot.class), Mockito.anyString()))
                .thenReturn(createdDTO);

        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Eisbach",
              "description": "Famous standing river wave in Munich.",
              "regionId": 1,
              "latitude": 48.1522,
              "longitude": 11.5884,
              "status": "Pending",
              "type": "Standing Wave",
              "beachBottomType": "Rock",
              "skillLevel": "Advanced",
              "waveDirection": "Right",
              "swellDirection": "W",
              "windDirection": "E",
              "userId": "test-user-id-123"
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/surf-spots/id/1?userId=test-user-id-123"))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.path", is("/surf-spots/europe/germany/bavaria/eisbach")))
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testCreateSurfSpotShouldReturnBadRequestWhenRiverWaveAndWavepoolBothTrue() throws Exception {
        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Wave Garden",
              "description": "Test novelty wave spot.",
              "regionId": 1,
              "status": "Pending",
              "isWavepool": true,
              "isRiverWave": true,
              "wavepoolUrl": "https://wavegarden.com/",
              "userId": "test-user-id-123"
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateSurfSpotShouldMapIsRiverWaveJsonProperty() throws Exception {
        SurfSpot surfSpot = SurfSpot.builder()
                .id(3L)
                .name("River Spot")
                .description("Test river wave spot.")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        SurfSpotDTO createdDTO = SurfSpotDTO.builder()
                .id(3L)
                .name("River Spot")
                .slug("river-spot")
                .path("/surf-spots/europe/germany/bavaria/river-spot")
                .isRiverWave(true)
                .isWavepool(false)
                .build();

        Mockito.when(surfSpotService.createSurfSpot(Mockito.any(SurfSpotRequest.class))).thenReturn(surfSpot);
        Mockito.when(surfSpotService.mapToSurfSpotDTO(Mockito.any(SurfSpot.class), Mockito.anyString()))
                .thenReturn(createdDTO);

        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "River Spot",
              "description": "Test river wave spot.",
              "regionId": 1,
              "status": "Pending",
              "isWavepool": false,
              "isRiverWave": true,
              "type": "Standing Wave",
              "beachBottomType": "Rock",
              "skillLevel": "Advanced",
              "waveDirection": "Right",
              "swellDirection": "W",
              "windDirection": "E",
              "userId": "test-user-id-123"
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        Mockito.verify(surfSpotService).createSurfSpot(Mockito.argThat(req ->
                req.isRiverWave() && !req.isWavepool()));
    }

    @Test
    void testCreateSurfSpotPublicPendingWithoutNoveltyOrOceanCoreShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Incomplete Public",
              "description": "Neither novelty flags nor ocean break/conditions",
              "regionId": 1,
              "latitude": 38.0,
              "longitude": -9.0,
              "status": "Pending",
              "userId": "test-user-id-123",
              "isWavepool": false,
              "isRiverWave": false
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateSurfSpotPublicWavepoolWithoutUrlShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Pool No Url",
              "description": "Wavepool flag but no website",
              "regionId": 1,
              "status": "Pending",
              "type": "Standing Wave",
              "beachBottomType": "Rock",
              "skillLevel": "Advanced",
              "waveDirection": "Right",
              "swellDirection": "W",
              "windDirection": "E",
              "userId": "test-user-id-123",
              "isWavepool": true,
              "isRiverWave": false
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateSurfSpotPublicWavepoolWithoutCoreFieldsShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Pool Missing Core Fields",
              "description": "Wavepool with URL but no break/conditions",
              "regionId": 1,
              "status": "Pending",
              "userId": "test-user-id-123",
              "isWavepool": true,
              "isRiverWave": false,
              "wavepoolUrl": "https://wavegarden.com/"
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateSurfSpotPublicWavepoolWithoutSwellAndWindShouldReturnCreatedSurfSpot() throws Exception {
        SurfSpot surfSpot = SurfSpot.builder()
                .id(4L)
                .name("Pool No Directions")
                .description("Wavepool with required core fields and URL only.")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        SurfSpotDTO createdDTO = SurfSpotDTO.builder()
                .id(4L)
                .name("Pool No Directions")
                .slug("pool-no-directions")
                .path("/surf-spots/europe/spain/basque-country/pool-no-directions")
                .isWavepool(true)
                .isRiverWave(false)
                .build();

        Mockito.when(surfSpotService.createSurfSpot(Mockito.any(SurfSpotRequest.class))).thenReturn(surfSpot);
        Mockito.when(surfSpotService.mapToSurfSpotDTO(Mockito.any(SurfSpot.class), Mockito.anyString()))
                .thenReturn(createdDTO);

        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Pool No Directions",
              "description": "Wavepool with required core fields and URL only.",
              "regionId": 1,
              "status": "Pending",
              "isWavepool": true,
              "isRiverWave": false,
              "wavepoolUrl": "https://wavegarden.com/",
              "type": "Standing Wave",
              "beachBottomType": "Rock",
              "skillLevel": "Advanced",
              "waveDirection": "Right",
              "userId": "test-user-id-123"
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testCreateSurfSpotPublicPendingWithBlankDescriptionShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/surf-spots/management")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "No Description Public",
              "description": "   ",
              "regionId": 1,
              "status": "Pending",
              "userId": "test-user-id-123",
              "isWavepool": false,
              "isRiverWave": true
            }
            """)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteSurfSpotShouldReturnSuccess() throws Exception {
        Mockito.doNothing().when(surfSpotService).deleteSurfSpot(1L, "test-user-id-123");

        mockMvc.perform(delete("/api/surf-spots/management/1")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .param("userId", "test-user-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is("Surf spot deleted successfully")));

        Mockito.verify(surfSpotService, Mockito.times(1)).deleteSurfSpot(1L, "test-user-id-123");
    }

    @Test
    void testUpdateSurfSpotShouldReturnForbiddenWhenUserIsNotOwner() throws Exception {
        Mockito.when(surfSpotService.updateSurfSpot(Mockito.eq(1L), Mockito.any(SurfSpotRequest.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You can only update surf spots you created"));

        mockMvc.perform(patch("/api/surf-spots/management/1")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("""
            {
              "name": "Updated Pipeline",
              "description": "An updated famous surf spot.",
              "regionId": 1,
              "status": "Private",
              "userId": "other-user-id"
            }
            """))
                .andExpect(status().isForbidden());
    }
}

