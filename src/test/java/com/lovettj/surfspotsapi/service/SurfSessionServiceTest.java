package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

import com.lovettj.surfspotsapi.dto.SurfSessionListItemDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.enums.WaveSize;
import com.lovettj.surfspotsapi.repository.SurfSessionMediaRepository;
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
    private SurfSessionMediaRepository surfSessionMediaRepository;
    @Mock
    private SurfSpotRepository surfSpotRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SurfboardRepository surfboardRepository;
    @Mock
    private UserSurfSpotService userSurfSpotService;
    @Mock
    private StorageService storageService;

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
        request.setTide(Tide.MID);
        request.setSwellDirection("N");
        request.setWindDirection("SW");

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
        verify(userSurfSpotService, never()).addUserSurfSpot(any(), any());
    }

    @Test
    void createSessionShouldOmitSurfboardWhenSurfboardIdBlank() {
        request.setSurfboardId("   ");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        surfSessionService.createSession(request);

        verify(surfboardRepository, never()).findByIdAndUserId(any(), any());
        verify(surfSessionRepository).save(any());
        verify(userSurfSpotService).addUserSurfSpot("u1", 10L);
    }

    @Test
    void createSessionShouldDefaultWouldSurfAgainWhenNullAndPersistNullDetailEnums() {
        request.setSurfboardId(null);
        request.setWaveSize(null);
        request.setCrowdLevel(null);
        request.setWaveQuality(null);
        request.setTide(null);
        request.setWouldSurfAgain(null);
        request.setSwellDirection(null);
        request.setWindDirection(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        surfSessionService.createSession(request);

        verify(surfSessionRepository)
                .save(
                        argThat(
                                s -> s.getWaveSize() == null
                                        && s.getCrowdLevel() == null
                                        && s.getWaveQuality() == null
                                        && s.getTide() == null
                                        && Boolean.FALSE.equals(s.getWouldSurfAgain())));
        verify(userSurfSpotService).addUserSurfSpot("u1", 10L);
    }

    @Test
    void createSessionShouldRequireSkillLevelWhenUserHasNoSkillLevel() {
        request.setSurfboardId(null);
        user.setSkillLevel(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.createSession(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(surfSessionRepository, never()).save(any());
        verify(userSurfSpotService, never()).addUserSurfSpot(any(), any());
    }

    @Test
    void createSessionShouldUseRequestSkillAndPersistUserWhenProfileSkillMissing() {
        request.setSurfboardId(null);
        request.setSkillLevel(SkillLevel.BEGINNER);
        user.setSkillLevel(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        surfSessionService.createSession(request);

        verify(surfSessionRepository).save(argThat(session ->
                session.getSkillLevel() == SkillLevel.BEGINNER));
        verify(userRepository).save(user);
        assertEquals(SkillLevel.BEGINNER, user.getSkillLevel());
        verify(userSurfSpotService).addUserSurfSpot("u1", 10L);
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

    @Test
    void listSessionsForUserShouldRejectBlankUserId() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.listSessionsForUser("  "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(surfSessionRepository, never()).findAllForUserList(any());
    }

    @Test
    void listSessionsForUserShouldRejectUnknownUser() {
        when(userRepository.existsById("missing")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.listSessionsForUser("missing"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(surfSessionRepository, never()).findAllForUserList(any());
    }

    @Test
    void listSessionsForUserShouldMapSpotPathAndFields() {
        Continent continent = new Continent();
        continent.setSlug("europe");
        Country country = new Country();
        country.setSlug("es");
        country.setContinent(continent);
        Region region = new Region();
        region.setSlug("andalusia");
        region.setCountry(country);
        SurfSpot spot = SurfSpot.builder().name("Test Break").build();
        spot.setId(5L);
        spot.setSlug("test-break");
        spot.setRegion(region);

        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(spot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2025, 6, 1))
                .waveSize(WaveSize.CHEST_SHOULDER)
                .crowdLevel(CrowdLevel.FEW)
                .waveQuality(WaveQuality.FUN)
                .wouldSurfAgain(true)
                .build();
        session.setId(1L);

        when(userRepository.existsById("u1")).thenReturn(true);
        when(surfSessionRepository.findAllForUserList("u1")).thenReturn(List.of(session));

        List<SurfSessionListItemDTO> list = surfSessionService.listSessionsForUser("u1");

        assertEquals(1, list.size());
        assertEquals("/surf-spots/europe/es/andalusia/test-break", list.get(0).getSpotPath());
        assertEquals("Test Break", list.get(0).getSurfSpotName());
        assertEquals(Long.valueOf(5L), list.get(0).getSurfSpotId());
        assertEquals(WaveQuality.FUN, list.get(0).getWaveQuality());
    }
}
