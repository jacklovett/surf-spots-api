package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.enums.WaveSize;
import com.lovettj.surfspotsapi.repository.SurfSessionRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.SurfboardRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;

@ExtendWith(MockitoExtension.class)
class SurfSessionServiceTest {

    @Mock
    private SurfSessionRepository surfSessionRepository;
    @Mock
    private SurfSpotRepository surfSpotRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SurfboardRepository surfboardRepository;

    @InjectMocks
    private SurfSessionService surfSessionService;

    private SurfSessionRequest request;
    private User user;
    @Mock
    private SurfSpot surfSpot;

    @BeforeEach
    void setUp() {
        request = new SurfSessionRequest();
        request.setSurfSpotId(10L);
        request.setUserId("u1");
        request.setSessionDate(LocalDate.of(2025, 4, 1));
        request.setWaveSize(WaveSize.SMALL);
        request.setCrowdLevel(CrowdLevel.EMPTY);
        request.setWaveQuality(WaveQuality.OKAY);
        request.setWouldSurfAgain(false);

        user = User.builder()
                .id("u1")
                .skillLevel(SkillLevel.INTERMEDIATE)
                .build();
    }

    @Test
    void createSessionShouldRejectSurfboardNotOwnedByUser() {
        request.setSurfboardId("board-1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));
        when(surfboardRepository.findByIdAndUserId("board-1", "u1")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> surfSessionService.createSession(request));
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void createSessionShouldOmitSurfboardWhenSurfboardIdBlank() {
        request.setSurfboardId("   ");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        surfSessionService.createSession(request);

        verify(surfboardRepository, never()).findByIdAndUserId(any(), any());
        verify(surfSessionRepository).save(any());
    }

    @Test
    void getSpotSummaryForUserShouldRejectBlankUserId() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.getSpotSummaryForUser(1L, "   "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getSpotSummaryForUserShouldRejectUnknownUser() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.getSpotSummaryForUser(1L, "missing"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getSpotSummaryForUserShouldResolveSkillFromDatabase() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSessionRepository.findBySurfSpotIdAndSkillLevel(1L, SkillLevel.INTERMEDIATE))
                .thenReturn(Collections.emptyList());
        when(surfSessionRepository.findBySurfSpotId(1L)).thenReturn(Collections.emptyList());

        surfSessionService.getSpotSummaryForUser(1L, "u1");

        verify(userRepository).findById("u1");
    }

    @Test
    void getSpotSummaryShouldUseEnumSummaryTrendLinesForDominantBuckets() {
        SurfSession s1 =
                SurfSession.builder()
                        .user(user)
                        .surfSpot(surfSpot)
                        .skillLevel(SkillLevel.INTERMEDIATE)
                        .sessionDate(LocalDate.of(2025, 1, 1))
                        .waveSize(WaveSize.SMALL)
                        .crowdLevel(CrowdLevel.BUSY)
                        .waveQuality(WaveQuality.FUN)
                        .wouldSurfAgain(true)
                        .build();
        SurfSession s2 =
                SurfSession.builder()
                        .user(user)
                        .surfSpot(surfSpot)
                        .skillLevel(SkillLevel.INTERMEDIATE)
                        .sessionDate(LocalDate.of(2025, 1, 2))
                        .waveSize(WaveSize.SMALL)
                        .crowdLevel(CrowdLevel.BUSY)
                        .waveQuality(WaveQuality.FUN)
                        .wouldSurfAgain(true)
                        .build();
        SurfSession s3 =
                SurfSession.builder()
                        .user(user)
                        .surfSpot(surfSpot)
                        .skillLevel(SkillLevel.INTERMEDIATE)
                        .sessionDate(LocalDate.of(2025, 1, 3))
                        .waveSize(WaveSize.SMALL)
                        .crowdLevel(CrowdLevel.BUSY)
                        .waveQuality(WaveQuality.FUN)
                        .wouldSurfAgain(true)
                        .build();
        // Need >= MIN_SAMPLE_FOR_SKILL_SEGMENT (3) or the service falls back to findBySurfSpotId, which must be stubbed separately.
        when(surfSessionRepository.findBySurfSpotIdAndSkillLevel(1L, SkillLevel.INTERMEDIATE))
                .thenReturn(List.of(s1, s2, s3));

        SurfSessionSummaryDTO dto = surfSessionService.getSpotSummary(1L, SkillLevel.INTERMEDIATE);

        assertNotNull(dto);
        assertEquals(WaveQuality.FUN.getSummaryTrendLine(), dto.getWaveQualityTrendLine());
        assertEquals(CrowdLevel.BUSY.getSummaryTrendLine(), dto.getCrowdTrendLine());
    }
}
