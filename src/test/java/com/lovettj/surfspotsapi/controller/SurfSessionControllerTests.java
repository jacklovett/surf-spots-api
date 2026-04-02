package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.dto.SurfSessionListItemDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.enums.WaveSize;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.service.SurfSessionService;

@SpringBootTest
@AutoConfigureMockMvc
class SurfSessionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SurfSessionService surfSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    private SurfSessionRequest validRequest;

    private Cookie sessionCookie() {
        return new Cookie("session", "testpayload.testsignature");
    }

    @BeforeEach
    void setUp() {
        validRequest = new SurfSessionRequest();
        validRequest.setSurfSpotId(1L);
        validRequest.setUserId("user-1");
        validRequest.setSessionDate(LocalDate.of(2025, 3, 20));
        validRequest.setWaveSize(WaveSize.CHEST_SHOULDER);
        validRequest.setCrowdLevel(CrowdLevel.FEW);
        validRequest.setWaveQuality(WaveQuality.FUN);
        validRequest.setWouldSurfAgain(true);
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
                .andExpect(status().isOk())
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(surfSessionService)
                .createSession(
                        argThat(
                                r -> r.getWaveSize() == null
                                        && r.getTide() == null
                                        && r.getWouldSurfAgain() == null));
    }

    @Test
    void testCreateSessionShouldAcceptGiantWaveSize() throws Exception {
        validRequest.setWaveSize(WaveSize.GIANT);
        doNothing().when(surfSessionService).createSession(any(SurfSessionRequest.class));

        mockMvc.perform(post("/api/surf-sessions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(surfSessionService).createSession(any(SurfSessionRequest.class));
    }

    @Test
    void testListMySessionsShouldReturnSessionsWhenServiceReturnsData() throws Exception {
        SurfSessionListItemDTO item = SurfSessionListItemDTO.builder()
                .id(1L)
                .sessionDate(LocalDate.of(2025, 6, 1))
                .surfSpotId(9L)
                .surfSpotName("Test Break")
                .spotPath("/surf-spots/europe/es/andalusia/test-break")
                .waveSize(WaveSize.CHEST_SHOULDER)
                .crowdLevel(CrowdLevel.FEW)
                .waveQuality(WaveQuality.FUN)
                .wouldSurfAgain(true)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .build();

        when(surfSessionService.listSessionsForUser("user-1")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/surf-sessions/mine")
                        .cookie(sessionCookie())
                        .param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].surfSpotName").value("Test Break"))
                .andExpect(jsonPath("$.data[0].spotPath").value("/surf-spots/europe/es/andalusia/test-break"));

        verify(surfSessionService).listSessionsForUser("user-1");
    }

    @Test
    void testListMySessionsShouldReturn400WhenUserIdMissing() throws Exception {
        mockMvc.perform(get("/api/surf-sessions/mine").cookie(sessionCookie()))
                .andExpect(status().isBadRequest());

        verify(surfSessionService, never()).listSessionsForUser(any());
    }

    @Test
    void testGetSpotSessionsSummaryShouldReturn400WhenUserIdMissing() throws Exception {
        mockMvc.perform(get("/api/surf-spots/5/sessions").cookie(sessionCookie()))
                .andExpect(status().isBadRequest());

        verify(surfSessionService, never()).getSpotSummaryForUser(any(), any());
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
    void testCreateSessionShouldRejectMissingSessionDate() throws Exception {
        validRequest.setSessionDate(null);

        mockMvc.perform(post("/api/surf-sessions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSpotSessionsSummaryShouldReturnWaveQualityDistribution() throws Exception {
        Map<String, Long> wq = new LinkedHashMap<>();
        wq.put("FUN", 2L);
        SurfSessionSummaryDTO dto = SurfSessionSummaryDTO.builder()
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sampleSize(2)
                .waveSizeDistribution(Map.of("CHEST_SHOULDER", 2L))
                .crowdDistribution(Map.of("BUSY", 2L))
                .waveQualityDistribution(wq)
                .wouldSurfAgainTrueCount(2L)
                .wouldSurfAgainFalseCount(0L)
                .fallbackToAllSkills(false)
                .segmentHeadline("Intermediate (2 sessions)")
                .waveQualityTrendLine("Mostly fun waves")
                .crowdTrendLine("Often busy")
                .wouldSurfAgainLine("2/2 would surf again")
                .build();

        when(surfSessionService.getSpotSummaryForUser(eq(99L), eq("user-1"))).thenReturn(dto);

        mockMvc.perform(get("/api/surf-spots/99/sessions")
                        .cookie(sessionCookie())
                        .param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waveQualityDistribution.FUN").value(2))
                .andExpect(jsonPath("$.segmentHeadline").value("Intermediate (2 sessions)"))
                .andExpect(jsonPath("$.waveQualityTrendLine").value("Mostly fun waves"));
    }
}
