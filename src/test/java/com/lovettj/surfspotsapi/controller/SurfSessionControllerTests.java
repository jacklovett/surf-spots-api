package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.dto.SurfSessionListItemDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionMediaDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.dto.UserSurfSessionsDTO;
import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.ExternalSessionProvider;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveSize;
import com.lovettj.surfspotsapi.requests.CreateSurfSessionMediaRequest;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;
import com.lovettj.surfspotsapi.requests.UploadMediaRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.service.SurfSessionService;
import com.lovettj.surfspotsapi.testutil.BaseControllerTest;
import com.lovettj.surfspotsapi.testutil.MockMvcDefaults;
import com.lovettj.surfspotsapi.testutil.SessionTestCookieFactory;

class SurfSessionControllerTests extends BaseControllerTest {

    private static final String TEST_USER_ID = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SurfSessionService surfSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    private SurfSessionRequest validRequest;

    private Cookie sessionCookie() {
        return SessionTestCookieFactory.createSignedSessionCookie(TEST_USER_ID);
    }

    @BeforeEach
    void setUp() {
        validRequest = new SurfSessionRequest();
        validRequest.setSurfSpotId(1L);
        validRequest.setUserId(TEST_USER_ID);
        validRequest.setSessionDate(LocalDate.of(2025, 3, 20));
        validRequest.setWaveSize(WaveSize.CHEST_SHOULDER);
        validRequest.setCrowdLevel(CrowdLevel.FEW);
        validRequest.setSessionRating(4);
        validRequest.setTide(Tide.MID);
        validRequest.setSwellDirection("N,NE");
        validRequest.setWindDirection("SW");
    }

    @Test
    void testCreateSessionShouldAcceptValidPayload() throws Exception {
        doNothing().when(surfSessionService).createSession(any(SurfSessionRequest.class));

        mockMvc.perform(post("/api/surf-sessions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201));

        verify(surfSessionService).createSession(any(SurfSessionRequest.class));
    }

    @Test
    void testCreateSessionShouldAcceptPayloadWithStartInstantAndNoSessionDate() throws Exception {
        SurfSessionRequest wearable = new SurfSessionRequest();
        wearable.setSurfSpotId(1L);
        wearable.setUserId(TEST_USER_ID);
        wearable.setSessionStartInstant(Instant.parse("2025-04-01T14:00:00Z"));
        wearable.setSessionEndInstant(Instant.parse("2025-04-01T15:00:00Z"));
        doNothing().when(surfSessionService).createSession(any(SurfSessionRequest.class));

        mockMvc.perform(post("/api/surf-sessions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wearable)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(surfSessionService).createSession(any(SurfSessionRequest.class));
    }

    @Test
    void testCreateSessionShouldAcceptMinimalPayloadWithSpotUserAndDate() throws Exception {
        SurfSessionRequest minimal = new SurfSessionRequest();
        minimal.setSurfSpotId(1L);
        minimal.setUserId("user-1");
        minimal.setSessionDate(LocalDate.of(2025, 4, 1));
        doNothing().when(surfSessionService).createSession(any(SurfSessionRequest.class));

        mockMvc.perform(post("/api/surf-sessions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(surfSessionService)
                .createSession(
                        argThat(
                                r -> r.getWaveSize() == null
                                        && r.getTide() == null
                                        && r.getWaveFace() == null
                                        && r.getSessionRating() == null));
    }

    @Test
    void testCreateSessionShouldAcceptGiantWaveSize() throws Exception {
        validRequest.setWaveSize(WaveSize.GIANT);
        doNothing().when(surfSessionService).createSession(any(SurfSessionRequest.class));

        mockMvc.perform(post("/api/surf-sessions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        verify(surfSessionService).createSession(any(SurfSessionRequest.class));
    }

    @Test
    void testGetSessionsForUserShouldReturnMinePayloadWhenServiceReturnsData() throws Exception {
        SurfSessionListItemDTO item = SurfSessionListItemDTO.builder()
                .id(1L)
                .sessionDate(LocalDate.of(2025, 6, 1))
                .surfSpotId(9L)
                .surfSpotName("Test Break")
                .spotPath("/surf-spots/europe/es/andalusia/test-break")
                .waveSize(WaveSize.CHEST_SHOULDER)
                .crowdLevel(CrowdLevel.FEW)
                .sessionRating(4)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .build();

        UserSurfSessionsDTO mine = UserSurfSessionsDTO.builder()
                .totalSessions(10L)
                .spotsSurfedCount(4L)
                .boardsUsedCount(3L)
                .sessions(List.of(item))
                .build();
        when(surfSessionService.getSurfSessionsForUser("user-1")).thenReturn(mine);

        mockMvc.perform(get("/api/surf-sessions")
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSessions").value(10))
                .andExpect(jsonPath("$.data.spotsSurfedCount").value(4))
                .andExpect(jsonPath("$.data.boardsUsedCount").value(3))
                .andExpect(jsonPath("$.data.sessions[0].surfSpotName").value("Test Break"))
                .andExpect(jsonPath("$.data.sessions[0].spotPath").value("/surf-spots/europe/es/andalusia/test-break"));

        verify(surfSessionService).getSurfSessionsForUser("user-1");
    }

    @Test
    void testGetSessionByIdShouldReturnOneSession() throws Exception {
        SurfSessionListItemDTO item = SurfSessionListItemDTO.builder()
                .id(5L)
                .sessionDate(LocalDate.of(2025, 7, 1))
                .surfSpotId(9L)
                .surfSpotName("Beach")
                .spotPath("/surf-spots/europe/es/andalusia/beach")
                .build();
        when(surfSessionService.getSessionByIdForUser("user-1", 5L)).thenReturn(item);

        mockMvc.perform(get("/api/surf-sessions/5").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.surfSpotName").value("Beach"));

        verify(surfSessionService).getSessionByIdForUser("user-1", 5L);
    }

    @Test
    void testGetSessionByIdShouldReturn404WhenNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.SURF_SESSION_NOT_FOUND))
                .when(surfSessionService)
                .getSessionByIdForUser("user-1", 99L);

        mockMvc.perform(get("/api/surf-sessions/99").cookie(sessionCookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ApiErrors.SURF_SESSION_NOT_FOUND));
    }

    @Test
    void testGetSessionByIdShouldReturn403WhenForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrors.SURF_SESSION_ACCESS_FORBIDDEN))
                .when(surfSessionService)
                .getSessionByIdForUser("user-1", 5L);

        mockMvc.perform(get("/api/surf-sessions/5").cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ApiErrors.SURF_SESSION_ACCESS_FORBIDDEN));
    }

    @Test
    void testUpdateSessionShouldReturnOk() throws Exception {
        doNothing().when(surfSessionService).updateSession(eq("user-1"), eq(2L), any(SurfSessionRequest.class));

        mockMvc.perform(put("/api/surf-sessions/2")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(surfSessionService).updateSession(eq("user-1"), eq(2L), any(SurfSessionRequest.class));
    }

    @Test
    void testDeleteSessionShouldReturnOk() throws Exception {
        doNothing().when(surfSessionService).deleteSession("user-1", 3L);

        mockMvc.perform(delete("/api/surf-sessions/3").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(surfSessionService).deleteSession("user-1", 3L);
    }

    @Test
    void testGetSessionsForUserShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/surf-sessions"))
                .andExpect(status().isForbidden());

        verify(surfSessionService, never()).getSurfSessionsForUser(any());
    }

    @Test
    void testGetSpotSessionsSummaryShouldResolveCurrentUserFromSession() throws Exception {
        when(surfSessionService.getSpotSummaryForUser(eq(5L), eq("user-1"))).thenReturn(
                SurfSessionSummaryDTO.builder()
                        .sampleSize(0)
                        .build());

        mockMvc.perform(get("/api/surf-spots/5/sessions").cookie(sessionCookie()))
                .andExpect(status().isOk());

        verify(surfSessionService).getSpotSummaryForUser(eq(5L), eq("user-1"));
    }

    @Test
    void testCreateSessionShouldReturn404WithReasonWhenServiceThrowsNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.USER_NOT_FOUND))
                .when(surfSessionService).createSession(any(SurfSessionRequest.class));

        mockMvc.perform(post("/api/surf-sessions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ApiErrors.USER_NOT_FOUND));
    }

    @Test
    void testCreateSessionShouldReturn409WhenServiceThrowsUniqueViolationWithExternalId() throws Exception {
        validRequest.setExternalSessionProvider(ExternalSessionProvider.GARMIN);
        validRequest.setExternalSessionId("activity-1");
        SQLException duplicateKey = new SQLException("duplicate key", "23505");
        doThrow(new DataIntegrityViolationException("unique", duplicateKey))
                .when(surfSessionService)
                .createSession(any(SurfSessionRequest.class));

        mockMvc.perform(post("/api/surf-sessions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ApiErrors.SURF_SESSION_ALREADY_SYNCED));
    }

    @Test
    void testCreateSessionShouldRejectMissingSessionDate() throws Exception {
        validRequest.setSessionDate(null);

        mockMvc.perform(post("/api/surf-sessions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSpotSessionsSummaryShouldReturnSessionRatingDistribution() throws Exception {
        Map<String, Long> ratingDistribution = new LinkedHashMap<>();
        ratingDistribution.put("4", 2L);
        SurfSessionSummaryDTO dto = SurfSessionSummaryDTO.builder()
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sampleSize(2)
                .waveSizeDistribution(Map.of("CHEST_SHOULDER", 2L))
                .crowdDistribution(Map.of("BUSY", 2L))
                .sessionRatingDistribution(ratingDistribution)
                .fallbackToAllSkills(false)
                .build();

        when(surfSessionService.getSpotSummaryForUser(eq(99L), eq("user-1"))).thenReturn(dto);

        mockMvc.perform(get("/api/surf-spots/99/sessions")
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionRatingDistribution['4']").value(2))
                .andExpect(jsonPath("$.sampleSize").value(2));
    }

    // --- POST /api/surf-sessions/{sessionId}/media/upload-url ---

    @Test
    void testGetMediaUploadUrlShouldReturnOkWhenAuthenticated() throws Exception {
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("image/jpeg");

        when(surfSessionService.getUploadUrl(anyString(), anyLong(), anyString(), anyString()))
                .thenReturn("https://example.com/upload/123");

        mockMvc.perform(post("/api/surf-sessions/5/media/upload-url")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadUrl").exists())
                .andExpect(jsonPath("$.data.mediaId").exists());
    }

    @Test
    void testGetMediaUploadUrlShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("image/jpeg");

        mockMvc.perform(post("/api/surf-sessions/5/media/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetMediaUploadUrlShouldReturnServiceUnavailableWhenStorageNotConfigured() throws Exception {
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("image/jpeg");

        when(surfSessionService.getUploadUrl(anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Media storage is not configured."));

        mockMvc.perform(post("/api/surf-sessions/5/media/upload-url")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(ApiErrors.MEDIA_UPLOAD_UNAVAILABLE));
    }

    // --- POST /api/surf-sessions/{sessionId}/media ---

    @Test
    void testAddMediaShouldReturnCreatedWhenAuthenticated() throws Exception {
        CreateSurfSessionMediaRequest request = new CreateSurfSessionMediaRequest();
        request.setMediaId(UUID.randomUUID().toString());
        request.setOriginalUrl("https://example.com/media/original.jpg");
        request.setThumbUrl("https://example.com/media/thumb.jpg");
        request.setMediaType("image/jpeg");

        SurfSessionMediaDTO mediaDTO = SurfSessionMediaDTO.builder()
                .id(request.getMediaId())
                .surfSessionId(5L)
                .originalUrl(request.getOriginalUrl())
                .mediaType("image/jpeg")
                .build();

        when(surfSessionService.addMedia(anyString(), anyLong(), any(CreateSurfSessionMediaRequest.class)))
                .thenReturn(mediaDTO);

        mockMvc.perform(post("/api/surf-sessions/5/media")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(request.getMediaId()));
    }

    @Test
    void testAddMediaShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        CreateSurfSessionMediaRequest request = new CreateSurfSessionMediaRequest();
        request.setMediaId(UUID.randomUUID().toString());
        request.setOriginalUrl("https://example.com/media/photo.jpg");
        request.setMediaType("image/jpeg");

        mockMvc.perform(post("/api/surf-sessions/5/media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /api/surf-sessions/media/{mediaId} ---

    @Test
    void testDeleteMediaShouldReturnOkWhenAuthenticated() throws Exception {
        String mediaId = UUID.randomUUID().toString();
        doNothing().when(surfSessionService).deleteMedia(anyString(), anyString());

        mockMvc.perform(delete("/api/surf-sessions/media/" + mediaId)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk());

        verify(surfSessionService).deleteMedia(TEST_USER_ID, mediaId);
    }

    @Test
    void testDeleteMediaShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        String mediaId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/surf-sessions/media/" + mediaId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteMediaShouldReturn404WhenMediaNotFound() throws Exception {
        String mediaId = UUID.randomUUID().toString();

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"))
                .when(surfSessionService).deleteMedia(anyString(), anyString());

        mockMvc.perform(delete("/api/surf-sessions/media/" + mediaId)
                        .cookie(sessionCookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Media not found"));
    }
}
