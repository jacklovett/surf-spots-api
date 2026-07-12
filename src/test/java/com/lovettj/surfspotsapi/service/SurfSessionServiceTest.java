package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.dto.SurfSessionListItemDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.dto.UserSurfSessionsDTO;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.SurfSessionMedia;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.ExternalSessionProvider;
import com.lovettj.surfspotsapi.enums.SessionStatus;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveSize;
import com.lovettj.surfspotsapi.repository.SurfSessionMediaRepository;
import com.lovettj.surfspotsapi.repository.SurfSessionRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.SurfboardRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.EndLiveSurfSessionRequest;
import com.lovettj.surfspotsapi.requests.LinkSessionsToSpotRequest;
import com.lovettj.surfspotsapi.requests.StartLiveSurfSessionRequest;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.util.SqlExceptionInspection;

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
    @Mock
    private SessionNotificationService sessionNotificationService;

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
        request.setSessionRating(3);
        request.setTide(Tide.MID);
        request.setSwellDirection("N");
        request.setWindDirection("SW");

        user = User.builder()
                .id("u1")
                .skillLevel(SkillLevel.INTERMEDIATE)
                .emailVerified(true)
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
    void createSessionShouldPersistNullDetailFieldsWhenOmitted() {
        request.setSurfboardId(null);
        request.setWaveSize(null);
        request.setCrowdLevel(null);
        request.setWaveFace(null);
        request.setSessionRating(null);
        request.setTide(null);
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
                                        && s.getWaveFace() == null
                                        && s.getSessionRating() == null
                                        && s.getTide() == null));
        verify(userSurfSpotService).addUserSurfSpot("u1", 10L);
    }

    @Test
    void createSessionShouldAllowNullSkillLevelWhenUserHasNoSkillLevel() {
        request.setSurfboardId(null);
        request.setSkillLevel(null);
        user.setSkillLevel(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        surfSessionService.createSession(request);

        verify(surfSessionRepository).save(argThat(session -> session.getSkillLevel() == null));
        verify(userRepository, never()).save(any());
        verify(userSurfSpotService).addUserSurfSpot("u1", 10L);
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
    void createSessionShouldDeriveDurationFromStartAndEnd() {
        request.setSurfboardId(null);
        request.setSessionStartTime(LocalTime.of(9, 0));
        request.setSessionEndTime(LocalTime.of(11, 30));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        surfSessionService.createSession(request);

        Instant expectedStart =
                ZonedDateTime.of(LocalDate.of(2025, 4, 1), LocalTime.of(9, 0), ZoneId.of("UTC")).toInstant();
        Instant expectedEnd =
                ZonedDateTime.of(LocalDate.of(2025, 4, 1), LocalTime.of(11, 30), ZoneId.of("UTC")).toInstant();

        verify(surfSessionRepository)
                .save(
                        argThat(
                                session ->
                                        Integer.valueOf(150).equals(session.getDurationMinutes())
                                                && expectedStart.equals(session.getSessionStartInstant())
                                                && expectedEnd.equals(session.getSessionEndInstant())));
        verify(userSurfSpotService).addUserSurfSpot("u1", 10L);
    }

    @Test
    void createSessionShouldPersistUtcInstantsWhenSpotHasIanaZoneAndManualTimes() {
        request.setSurfboardId(null);
        request.setSessionStartTime(LocalTime.of(9, 0));
        request.setSessionEndTime(LocalTime.of(11, 30));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));
        when(surfSpot.getIanaZoneId()).thenReturn("Australia/Sydney");

        Instant expectedStart =
                ZonedDateTime.of(
                                LocalDate.of(2025, 4, 1),
                                LocalTime.of(9, 0),
                                ZoneId.of("Australia/Sydney"))
                        .toInstant();
        Instant expectedEnd =
                ZonedDateTime.of(
                                LocalDate.of(2025, 4, 1),
                                LocalTime.of(11, 30),
                                ZoneId.of("Australia/Sydney"))
                        .toInstant();

        surfSessionService.createSession(request);

        verify(surfSessionRepository)
                .save(
                        argThat(
                                session ->
                                        expectedStart.equals(session.getSessionStartInstant())
                                                && expectedEnd.equals(session.getSessionEndInstant())));
        verify(userSurfSpotService).addUserSurfSpot("u1", 10L);
    }

    @Test
    void createSessionShouldDeriveLocalTimesFromWearableInstantsUsingSpotZone() {
        request.setSurfboardId(null);
        request.setSessionDate(null);
        request.setSessionStartTime(null);
        request.setSessionEndTime(null);
        request.setSessionStartInstant(Instant.parse("2025-04-01T14:00:00Z"));
        request.setSessionEndInstant(Instant.parse("2025-04-01T16:30:00Z"));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));
        when(surfSpot.getIanaZoneId()).thenReturn("UTC");

        surfSessionService.createSession(request);

        verify(surfSessionRepository)
                .save(
                        argThat(
                                session ->
                                        LocalDate.of(2025, 4, 1).equals(session.getSessionDate())
                                                && Integer.valueOf(150).equals(session.getDurationMinutes())
                                                && Instant.parse("2025-04-01T14:00:00Z")
                                                        .equals(session.getSessionStartInstant())
                                                && Instant.parse("2025-04-01T16:30:00Z")
                                                        .equals(session.getSessionEndInstant())));
        verify(userSurfSpotService).addUserSurfSpot("u1", 10L);
    }

    @Test
    void createSessionShouldAllowStartTimeOnlyWithNullDuration() {
        request.setSurfboardId(null);
        request.setSessionStartTime(LocalTime.of(6, 30));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        surfSessionService.createSession(request);

        verify(surfSessionRepository)
                .save(
                        argThat(
                                session ->
                                        session.getDurationMinutes() == null
                                                && LocalTime.of(6, 30)
                                                        .equals(
                                                                ZonedDateTime.ofInstant(
                                                                                session.getSessionStartInstant(),
                                                                                ZoneId.of("UTC"))
                                                                        .toLocalTime())
                                                && session.getSessionEndInstant() == null));
    }

    @Test
    void createSessionShouldRejectEndWithoutStart() {
        request.setSurfboardId(null);
        request.setSessionEndTime(LocalTime.of(11, 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.createSession(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void createSessionShouldRejectWhenEndEqualsStart() {
        request.setSurfboardId(null);
        request.setSessionStartTime(LocalTime.of(10, 0));
        request.setSessionEndTime(LocalTime.of(10, 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.createSession(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void createSessionShouldRejectWhenEndBeforeStart() {
        request.setSurfboardId(null);
        request.setSessionStartTime(LocalTime.of(14, 0));
        request.setSessionEndTime(LocalTime.of(9, 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.createSession(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void createSessionShouldRejectDuplicateExternalKeyForSameUserAndProvider() {
        request.setSurfboardId(null);
        request.setExternalSessionProvider(ExternalSessionProvider.SURFLINE);
        request.setExternalSessionId("healthkit-workout-1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));
        when(surfSessionRepository.externalSessionAlreadyRecordedForUser(
                        "u1", ExternalSessionProvider.SURFLINE, "healthkit-workout-1"))
                .thenReturn(true);

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.createSession(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(ApiErrors.SURF_SESSION_ALREADY_SYNCED, ex.getReason());
        verify(surfSessionRepository, never()).save(any());
        verify(userSurfSpotService, never()).addUserSurfSpot(any(), any());
    }

    @Test
    void createSessionShouldReturnConflictWhenFlushHitsUniqueViolationForExternalSync() {
        request.setSurfboardId(null);
        request.setExternalSessionProvider(ExternalSessionProvider.RIP_CURL_SEARCH_GPS3);
        request.setExternalSessionId("race");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));
        when(surfSessionRepository.externalSessionAlreadyRecordedForUser(
                        "u1", ExternalSessionProvider.RIP_CURL_SEARCH_GPS3, "race"))
                .thenReturn(false);
        SQLException duplicateKey =
                new SQLException(
                        "duplicate key value violates unique constraint \""
                                + SqlExceptionInspection.UQ_SURF_SESSION_USER_PROVIDER_EXTERNAL
                                + "\"",
                        "23505");
        when(surfSessionRepository.save(any(SurfSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("unique violation", duplicateKey))
                .when(surfSessionRepository)
                .flush();

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.createSession(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(ApiErrors.SURF_SESSION_ALREADY_SYNCED, ex.getReason());
        verify(userSurfSpotService, never()).addUserSurfSpot(any(), any());
    }

    @Test
    void createSessionShouldRejectExternalIdWithoutProvider() {
        request.setSurfboardId(null);
        request.setExternalSessionId("orphan-id");
        request.setExternalSessionProvider(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.createSession(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(ApiErrors.EXTERNAL_SESSION_SYNC_PAIR_REQUIRED, ex.getReason());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void createSessionShouldRejectProviderWithoutExternalId() {
        request.setSurfboardId(null);
        request.setExternalSessionProvider(ExternalSessionProvider.GARMIN);
        request.setExternalSessionId(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.createSession(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(ApiErrors.EXTERNAL_SESSION_SYNC_PAIR_REQUIRED, ex.getReason());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void createSessionShouldAllowSameExternalIdWhenProvidersDiffer() {
        request.setSurfboardId(null);
        request.setExternalSessionProvider(ExternalSessionProvider.SURFLINE);
        request.setExternalSessionId("workout-99");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));
        when(surfSessionRepository.externalSessionAlreadyRecordedForUser(
                        "u1", ExternalSessionProvider.SURFLINE, "workout-99"))
                .thenReturn(false);

        surfSessionService.createSession(request);

        verify(surfSessionRepository)
                .save(argThat(session -> ExternalSessionProvider.SURFLINE.equals(session.getExternalSessionProvider())
                        && "workout-99".equals(session.getExternalSessionId())));

        request.setExternalSessionProvider(ExternalSessionProvider.RIP_CURL_SEARCH_GPS3);
        when(surfSessionRepository.externalSessionAlreadyRecordedForUser(
                        "u1", ExternalSessionProvider.RIP_CURL_SEARCH_GPS3, "workout-99"))
                .thenReturn(false);

        surfSessionService.createSession(request);

        verify(surfSessionRepository, times(2)).save(any());
        verify(userSurfSpotService, times(2)).addUserSurfSpot("u1", 10L);
    }

    @Test
    void getSpotSummaryForUserShouldRejectBlankUserId() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.getSpotSummaryForUser(1L, "  "));
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
    void getSpotSummaryShouldReturnSessionRatingDistribution() {
        SurfSession s1 =
                SurfSession.builder()
                        .user(user)
                        .surfSpot(surfSpot)
                        .skillLevel(SkillLevel.INTERMEDIATE)
                        .sessionDate(LocalDate.of(2025, 1, 1))
                        .waveSize(WaveSize.SMALL)
                        .crowdLevel(CrowdLevel.BUSY)
                        .sessionRating(4)
                        .build();
        SurfSession s2 =
                SurfSession.builder()
                        .user(user)
                        .surfSpot(surfSpot)
                        .skillLevel(SkillLevel.INTERMEDIATE)
                        .sessionDate(LocalDate.of(2025, 1, 2))
                        .waveSize(WaveSize.SMALL)
                        .crowdLevel(CrowdLevel.BUSY)
                        .sessionRating(4)
                        .build();
        SurfSession s3 =
                SurfSession.builder()
                        .user(user)
                        .surfSpot(surfSpot)
                        .skillLevel(SkillLevel.INTERMEDIATE)
                        .sessionDate(LocalDate.of(2025, 1, 3))
                        .waveSize(WaveSize.SMALL)
                        .crowdLevel(CrowdLevel.BUSY)
                        .sessionRating(5)
                        .build();
        // Need >= MIN_SAMPLE_FOR_SKILL_SEGMENT (3) or the service falls back to findBySurfSpotId, which must be stubbed separately.
        when(surfSessionRepository.findBySurfSpotIdAndSkillLevel(1L, SkillLevel.INTERMEDIATE))
                .thenReturn(List.of(s1, s2, s3));

        SurfSessionSummaryDTO dto = surfSessionService.getSpotSummary(1L, SkillLevel.INTERMEDIATE);

        assertNotNull(dto);
        assertEquals(2L, dto.getSessionRatingDistribution().get("4"));
        assertEquals(1L, dto.getSessionRatingDistribution().get("5"));
        assertEquals(3L, dto.getCrowdDistribution().get("BUSY"));
    }

    @Test
    void getSurfSessionsForUserShouldRejectBlankUserId() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.getSurfSessionsForUser("  "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(surfSessionRepository, never()).findAllForUserList(any());
    }

    @Test
    void getSurfSessionsForUserShouldRejectUnknownUser() {
        when(userRepository.existsById("missing")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.getSurfSessionsForUser("missing"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(surfSessionRepository, never()).findAllForUserList(any());
    }

    @Test
    void getSurfSessionsForUserShouldMapSpotPathAndFields() {
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
                .sessionRating(4)
                .build();
        session.setId(1L);
        session.setDurationMinutes(60);
        session.setSessionStartInstant(
                ZonedDateTime.of(LocalDate.of(2025, 6, 1), LocalTime.of(8, 0), ZoneId.of("UTC")).toInstant());
        session.setSessionEndInstant(
                ZonedDateTime.of(LocalDate.of(2025, 6, 1), LocalTime.of(9, 0), ZoneId.of("UTC")).toInstant());

        when(userRepository.existsById("u1")).thenReturn(true);
        when(surfSessionRepository.findAllForUserList("u1")).thenReturn(List.of(session));
        when(surfSessionRepository.countAllByUserId("u1")).thenReturn(1L);
        when(surfSessionRepository.countDistinctSurfSpotsByUserId("u1")).thenReturn(1L);
        when(surfSessionRepository.countDistinctBoardsByUserId("u1")).thenReturn(0L);

        UserSurfSessionsDTO mine = surfSessionService.getSurfSessionsForUser("u1");

        assertEquals(1, mine.getSessions().size());
        assertEquals("/surf-spots/europe/es/andalusia/test-break", mine.getSessions().get(0).getSpotPath());
        assertEquals("Test Break", mine.getSessions().get(0).getSurfSpotName());
        assertEquals(Long.valueOf(5L), mine.getSessions().get(0).getSurfSpotId());
        assertEquals(Integer.valueOf(4), mine.getSessions().get(0).getSessionRating());
        assertEquals(60, mine.getSessions().get(0).getDurationMinutes().intValue());
        assertEquals(LocalTime.of(8, 0), mine.getSessions().get(0).getSessionStartTime());
        assertEquals(LocalTime.of(9, 0), mine.getSessions().get(0).getSessionEndTime());
        assertEquals(1L, mine.getTotalSessions());
        assertEquals(1L, mine.getSpotsSurfedCount());
        assertEquals(0L, mine.getBoardsUsedCount());
    }

    @Test
    void getSurfSessionsForUserShouldReturnAggregatedCountsWithSessionsList() {
        when(userRepository.existsById("u1")).thenReturn(true);
        when(surfSessionRepository.findAllForUserList("u1")).thenReturn(Collections.emptyList());
        when(surfSessionRepository.countAllByUserId("u1")).thenReturn(7L);
        when(surfSessionRepository.countDistinctSurfSpotsByUserId("u1")).thenReturn(3L);
        when(surfSessionRepository.countDistinctBoardsByUserId("u1")).thenReturn(2L);

        UserSurfSessionsDTO mine = surfSessionService.getSurfSessionsForUser("u1");

        assertNotNull(mine.getSessions());
        assertEquals(7L, mine.getTotalSessions());
        assertEquals(3L, mine.getSpotsSurfedCount());
        assertEquals(2L, mine.getBoardsUsedCount());
    }

    @Test
    void getSurfSessionsForUserShouldReturnZerosWhenNoSessionsExist() {
        when(userRepository.existsById("u1")).thenReturn(true);
        when(surfSessionRepository.findAllForUserList("u1")).thenReturn(Collections.emptyList());
        when(surfSessionRepository.countAllByUserId("u1")).thenReturn(0L);
        when(surfSessionRepository.countDistinctSurfSpotsByUserId("u1")).thenReturn(0L);
        when(surfSessionRepository.countDistinctBoardsByUserId("u1")).thenReturn(0L);

        UserSurfSessionsDTO mine = surfSessionService.getSurfSessionsForUser("u1");

        assertEquals(0L, mine.getTotalSessions());
        assertEquals(0L, mine.getSpotsSurfedCount());
        assertEquals(0L, mine.getBoardsUsedCount());
        assertEquals(0, mine.getSessions().size());
    }

    @Test
    void getSurfSessionsForUserShouldIncludeInProgressSessionWithoutSurfSpot() {
        Instant start = Instant.parse("2026-07-01T06:00:00Z");
        SurfSession liveSession = SurfSession.builder()
                .user(user)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(start)
                .status(SessionStatus.IN_PROGRESS)
                .startLatitude(54.4783)
                .startLongitude(-8.2779)
                .build();
        liveSession.setId(42L);

        when(userRepository.existsById("u1")).thenReturn(true);
        when(surfSessionRepository.findAllForUserList("u1")).thenReturn(List.of(liveSession));
        when(surfSessionRepository.countAllByUserId("u1")).thenReturn(1L);
        when(surfSessionRepository.countDistinctSurfSpotsByUserId("u1")).thenReturn(0L);
        when(surfSessionRepository.countDistinctBoardsByUserId("u1")).thenReturn(0L);

        UserSurfSessionsDTO mine = surfSessionService.getSurfSessionsForUser("u1");

        assertEquals(1, mine.getSessions().size());
        assertEquals(SessionStatus.IN_PROGRESS, mine.getSessions().get(0).getStatus());
        assertNull(mine.getSessions().get(0).getSurfSpotId());
        assertEquals("Live session", mine.getSessions().get(0).getSurfSpotName());
        assertEquals(1L, mine.getTotalSessions());
    }

    @Test
    void getInProgressSessionForUserShouldReturnSessionWithoutSurfSpot() {
        Instant start = Instant.parse("2026-07-01T06:00:00Z");
        SurfSession liveSession = SurfSession.builder()
                .user(user)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(start)
                .status(SessionStatus.IN_PROGRESS)
                .startLatitude(54.4783)
                .startLongitude(-8.2779)
                .build();
        liveSession.setId(42L);

        when(surfSessionRepository.findFirstByUserIdAndStatusOrderBySessionStartInstantDescCreatedAtDesc(
                        "u1", SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(liveSession));

        SurfSessionListItemDTO inProgress = surfSessionService.getInProgressSessionForUser("u1");

        assertEquals(Long.valueOf(42L), inProgress.getId());
        assertEquals(SessionStatus.IN_PROGRESS, inProgress.getStatus());
        assertNull(inProgress.getSurfSpotId());
        assertEquals("Live session", inProgress.getSurfSpotName());
    }

    @Test
    void getSessionByIdForUserShouldRejectWhenMissing() {
        when(surfSessionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.getSessionByIdForUser("u1", 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getSessionByIdForUserShouldRejectWhenOwnerMismatch() {
        User otherUser = User.builder().id("other").skillLevel(SkillLevel.BEGINNER).emailVerified(true).build();
        SurfSession session = SurfSession.builder()
                .user(otherUser)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.BEGINNER)
                .sessionDate(LocalDate.of(2025, 1, 1))
                .build();
        session.setId(7L);
        when(surfSessionRepository.findById(7L)).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.getSessionByIdForUser("u1", 7L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateSessionShouldRejectWhenSurfSpotIdDoesNotMatchSession() {
        when(surfSpot.getId()).thenReturn(10L);
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2025, 4, 1))
                .build();
        session.setId(3L);
        when(surfSessionRepository.findById(3L)).thenReturn(Optional.of(session));
        request.setSurfSpotId(11L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSessionService.updateSession("u1", 3L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void updateSessionShouldAllowLiveSessionSurfSpotReassignment() {
        SurfSpot newSpot = SurfSpot.builder().id(11L).ianaZoneId("UTC").build();
        when(surfSpot.getId()).thenReturn(10L);
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2025, 4, 1))
                .startLatitude(48.5)
                .startLongitude(-4.5)
                .sessionStartInstant(Instant.parse("2025-04-01T09:00:00Z"))
                .sessionEndInstant(Instant.parse("2025-04-01T10:00:00Z"))
                .build();
        session.setId(3L);
        when(surfSessionRepository.findById(3L)).thenReturn(Optional.of(session));
        when(surfSpotRepository.findById(11L)).thenReturn(Optional.of(newSpot));
        request.setSurfSpotId(11L);
        request.setSessionNotes("Updated spot");

        surfSessionService.updateSession("u1", 3L, request);

        assertEquals(newSpot, session.getSurfSpot());
        assertEquals("Updated spot", session.getSessionNotes());
        verify(userSurfSpotService).addUserSurfSpot("u1", 11L);
        verify(surfSessionRepository).save(session);
    }

    @Test
    void updateSessionShouldClearLiveSessionSurfSpotWhenRequestOmitsSpotId() {
        when(surfSpot.getId()).thenReturn(10L);
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2025, 4, 1))
                .startLatitude(48.5)
                .startLongitude(-4.5)
                .sessionStartInstant(Instant.parse("2025-04-01T09:00:00Z"))
                .sessionEndInstant(Instant.parse("2025-04-01T10:00:00Z"))
                .build();
        session.setId(3L);
        when(surfSessionRepository.findById(3L)).thenReturn(Optional.of(session));
        request.setSurfSpotId(null);

        surfSessionService.updateSession("u1", 3L, request);

        assertNull(session.getSurfSpot());
        verify(surfSessionRepository).save(session);
    }

    @Test
    void deleteSessionShouldDeleteStoredMediaThenSession() {
        SurfSessionMedia media = SurfSessionMedia.builder()
                .id("media-a")
                .originalUrl("https://bucket/key")
                .mediaType("image")
                .build();
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2025, 4, 1))
                .build();
        session.setId(8L);
        media.setSurfSession(session);
        session.setMedia(List.of(media));
        when(surfSessionRepository.findById(8L)).thenReturn(Optional.of(session));
        when(storageService.resolveObjectKeyWithFallback(
                null, "https://bucket/key", "media-a", "image", "surf-sessions/media"))
                .thenReturn("surf-sessions/media/media-a");
        when(storageService.deleteObject("surf-sessions/media/media-a")).thenReturn(true);

        surfSessionService.deleteSession("u1", 8L);

        verify(storageService).deleteObject("surf-sessions/media/media-a");
        verify(surfSessionRepository).delete(session);
    }

    @Test
    void updateSessionShouldPersistNotesWhenSurfSpotMatches() {
        when(surfSpot.getId()).thenReturn(10L);
        when(surfSpot.getIanaZoneId()).thenReturn("UTC");
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2025, 4, 1))
                .sessionNotes("Old")
                .build();
        session.setId(3L);
        when(surfSessionRepository.findById(3L)).thenReturn(Optional.of(session));
        request.setSurfSpotId(10L);
        request.setSessionNotes("New notes");

        surfSessionService.updateSession("u1", 3L, request);

        assertEquals("New notes", session.getSessionNotes());
        verify(surfSessionRepository).save(session);
    }

    @Test
    void updateSessionShouldAllowNullSkillWhenProfileAndSessionHaveNoSkill() {
        when(surfSpot.getId()).thenReturn(10L);
        when(surfSpot.getIanaZoneId()).thenReturn("UTC");
        user.setSkillLevel(null);
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(null)
                .sessionDate(LocalDate.of(2025, 4, 1))
                .sessionNotes("Old")
                .build();
        session.setId(3L);
        when(surfSessionRepository.findById(3L)).thenReturn(Optional.of(session));
        request.setSurfSpotId(10L);
        request.setSkillLevel(null);
        request.setSessionNotes("Updated without skill");

        surfSessionService.updateSession("u1", 3L, request);

        assertNull(session.getSkillLevel());
        assertEquals("Updated without skill", session.getSessionNotes());
        verify(userRepository, never()).save(any());
        verify(surfSessionRepository).save(session);
    }

    @Test
    void updateSessionShouldPreserveStoredInstantsWhenRequestOmitsTimingFields() {
        when(surfSpot.getId()).thenReturn(10L);
        Instant start = Instant.parse("2025-04-01T14:00:00Z");
        Instant end = Instant.parse("2025-04-01T15:30:00Z");
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2025, 4, 1))
                .durationMinutes(90)
                .sessionStartInstant(start)
                .sessionEndInstant(end)
                .sessionNotes("Imported")
                .waveSize(WaveSize.SMALL)
                .crowdLevel(CrowdLevel.EMPTY)
                .sessionRating(3)
                .tide(Tide.MID)
                .build();
        session.setId(3L);
        when(surfSessionRepository.findById(3L)).thenReturn(Optional.of(session));

        SurfSessionRequest patch = new SurfSessionRequest();
        patch.setSurfSpotId(10L);
        patch.setUserId("u1");
        patch.setSessionDate(LocalDate.of(2025, 4, 1));
        patch.setSkillLevel(SkillLevel.INTERMEDIATE);
        patch.setWaveSize(WaveSize.SMALL);
        patch.setCrowdLevel(CrowdLevel.EMPTY);
        patch.setSessionRating(3);
        patch.setTide(Tide.MID);
        patch.setSwellDirection("N");
        patch.setWindDirection("SW");
        patch.setSessionNotes("Edited notes only");

        surfSessionService.updateSession("u1", 3L, patch);

        assertEquals(start, session.getSessionStartInstant());
        assertEquals(end, session.getSessionEndInstant());
        assertEquals(Integer.valueOf(90), session.getDurationMinutes());
        assertEquals("Edited notes only", session.getSessionNotes());
        verify(surfSessionRepository).save(session);
    }

    @Test
    void deleteSessionShouldRejectWhenSessionInProgress() {
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2025, 4, 1))
                .status(SessionStatus.IN_PROGRESS)
                .build();
        session.setId(8L);
        when(surfSessionRepository.findById(8L)).thenReturn(Optional.of(session));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.deleteSession("u1", 8L));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(ApiErrors.SURF_SESSION_IN_PROGRESS_CANNOT_DELETE, exception.getReason());
        verify(surfSessionRepository, never()).delete(any());
    }

    @Test
    void deleteSessionWithNoMediaShouldDeleteWithoutStorageCalls() {
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2025, 4, 1))
                .build();
        session.setId(8L);
        session.setMedia(Collections.emptyList());
        when(surfSessionRepository.findById(8L)).thenReturn(Optional.of(session));

        surfSessionService.deleteSession("u1", 8L);

        verify(storageService, never()).deleteObject(anyString());
        verify(surfSessionRepository).delete(session);
    }

    @Test
    void startLiveSessionShouldRejectWhenAnotherSessionInProgress() {
        when(surfSessionRepository.existsByUserIdAndStatus("u1", SessionStatus.IN_PROGRESS)).thenReturn(true);

        StartLiveSurfSessionRequest startRequest = new StartLiveSurfSessionRequest();
        startRequest.setStartLatitude(54.4783);
        startRequest.setStartLongitude(-8.2779);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.startLiveSession("u1", startRequest));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(ApiErrors.SURF_SESSION_ALREADY_IN_PROGRESS, exception.getReason());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void startLiveSessionShouldCreateInProgressSessionWithCoordinatesAndNotifyContact() {
        when(surfSessionRepository.existsByUserIdAndStatus("u1", SessionStatus.IN_PROGRESS)).thenReturn(false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        StartLiveSurfSessionRequest startRequest = new StartLiveSurfSessionRequest();
        startRequest.setStartLatitude(54.4783);
        startRequest.setStartLongitude(-8.2779);
        startRequest.setStartIanaZoneId("Europe/Dublin");
        startRequest.setShareLocationWithEmergencyContact(true);
        startRequest.setExpectedReturnInstant(Instant.now().plus(2, ChronoUnit.HOURS));

        SurfSessionListItemDTO created = surfSessionService.startLiveSession("u1", startRequest);

        assertNotNull(created);
        assertEquals(SessionStatus.IN_PROGRESS, created.getStatus());
        assertNull(created.getSurfSpotId());
        assertEquals("Live session", created.getSurfSpotName());
        verify(surfSessionRepository).save(argThat(session ->
                session.getSurfSpot() == null
                        && session.getStatus() == SessionStatus.IN_PROGRESS
                        && session.getSessionStartInstant() != null
                        && session.getSessionEndInstant() == null
                        && session.isShareLocationWithEmergencyContact()
                        && Double.valueOf(54.4783).equals(session.getStartLatitude())
                        && Double.valueOf(-8.2779).equals(session.getStartLongitude())
                        && "Europe/Dublin".equals(session.getStartIanaZoneId())));
        verify(userSurfSpotService, never()).addUserSurfSpot(any(), any());
        verify(sessionNotificationService).notifySessionStarted(
                eq(user), argThat(session -> session.getStatus() == SessionStatus.IN_PROGRESS));
    }

    @Test
    void startLiveSessionShouldAllowStartWithoutProfileSkillLevel() {
        User userWithoutSkill = User.builder()
                .id("u1")
                .emailVerified(true)
                .build();
        when(surfSessionRepository.existsByUserIdAndStatus("u1", SessionStatus.IN_PROGRESS)).thenReturn(false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(userWithoutSkill));

        StartLiveSurfSessionRequest startRequest = new StartLiveSurfSessionRequest();
        startRequest.setStartLatitude(54.4783);
        startRequest.setStartLongitude(-8.2779);

        SurfSessionListItemDTO created = surfSessionService.startLiveSession("u1", startRequest);

        assertNotNull(created);
        assertEquals(SessionStatus.IN_PROGRESS, created.getStatus());
        verify(surfSessionRepository).save(argThat(session ->
                session.getStatus() == SessionStatus.IN_PROGRESS
                        && session.getSkillLevel() == null
                        && session.getSurfSpot() == null));
        verify(userRepository, never()).save(any());
        verify(userSurfSpotService, never()).addUserSurfSpot(any(), any());
    }

    @Test
    void endLiveSessionShouldCompleteWhenSkillLevelMissingFromProfileAndSession() {
        User userWithoutSkill = User.builder()
                .id("u1")
                .emailVerified(true)
                .build();
        Instant start = Instant.now().minus(45, ChronoUnit.MINUTES);
        SurfSession session = SurfSession.builder()
                .user(userWithoutSkill)
                .surfSpot(surfSpot)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(start)
                .startLatitude(54.4783)
                .startLongitude(-8.2779)
                .status(SessionStatus.IN_PROGRESS)
                .build();
        session.setId(9L);
        when(surfSessionRepository.findByIdWithUserForUpdate(9L)).thenReturn(Optional.of(session));

        EndLiveSurfSessionRequest endRequest = new EndLiveSurfSessionRequest();
        endRequest.setWaveSize(WaveSize.SMALL);

        SurfSessionListItemDTO ended = surfSessionService.endLiveSession("u1", 9L, endRequest);

        assertEquals(SessionStatus.COMPLETED, ended.getStatus());
        assertNull(session.getSkillLevel());
        verify(surfSessionRepository).save(session);
    }

    @Test
    void endLiveSessionShouldCompleteSessionAndNotifyContact() {
        Instant start = Instant.now().minus(45, ChronoUnit.MINUTES);
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(start)
                .startLatitude(54.4783)
                .startLongitude(-8.2779)
                .status(SessionStatus.IN_PROGRESS)
                .shareLocationWithEmergencyContact(true)
                .build();
        session.setId(9L);
        when(surfSpot.getId()).thenReturn(10L);
        when(surfSessionRepository.findByIdWithUserForUpdate(9L)).thenReturn(Optional.of(session));

        EndLiveSurfSessionRequest endRequest = new EndLiveSurfSessionRequest();
        endRequest.setWaveSize(WaveSize.SMALL);
        endRequest.setCrowdLevel(CrowdLevel.EMPTY);
        endRequest.setSessionNotes("Fun waves");

        SurfSessionListItemDTO ended = surfSessionService.endLiveSession("u1", 9L, endRequest);

        assertEquals(SessionStatus.COMPLETED, ended.getStatus());
        assertEquals("Fun waves", session.getSessionNotes());
        assertNotNull(session.getSessionEndInstant());
        assertNotNull(session.getDurationMinutes());
        verify(sessionNotificationService).notifySessionEnded(user, session);
        verify(surfSessionRepository).save(session);
    }

    @Test
    void endLiveSessionShouldAttachSurfSpotWhenProvided() {
        Instant start = Instant.now().minus(45, ChronoUnit.MINUTES);
        SurfSession session = SurfSession.builder()
                .user(user)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(start)
                .startLatitude(54.4783)
                .startLongitude(-8.2779)
                .status(SessionStatus.IN_PROGRESS)
                .build();
        session.setId(9L);
        when(surfSessionRepository.findByIdWithUserForUpdate(9L)).thenReturn(Optional.of(session));
        when(surfSpotRepository.findById(10L)).thenReturn(Optional.of(surfSpot));
        when(surfSpot.getId()).thenReturn(10L);

        EndLiveSurfSessionRequest endRequest = new EndLiveSurfSessionRequest();
        endRequest.setSurfSpotId(10L);
        endRequest.setSessionRating(4);

        SurfSessionListItemDTO ended = surfSessionService.endLiveSession("u1", 9L, endRequest);

        assertEquals(SessionStatus.COMPLETED, ended.getStatus());
        assertEquals(surfSpot, session.getSurfSpot());
        verify(userSurfSpotService).addUserSurfSpot("u1", 10L);
        verify(surfSessionRepository).save(session);
    }

    @Test
    void endLiveSessionShouldReturnIdempotentlyWhenAlreadyCompletedLiveSession() {
        Instant start = Instant.now().minus(45, ChronoUnit.MINUTES);
        Instant end = Instant.now().minus(5, ChronoUnit.MINUTES);
        SurfSession session = SurfSession.builder()
                .user(user)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(start)
                .sessionEndInstant(end)
                .durationMinutes(40)
                .startLatitude(54.4783)
                .startLongitude(-8.2779)
                .status(SessionStatus.COMPLETED)
                .build();
        session.setId(9L);
        when(surfSessionRepository.findByIdWithUserForUpdate(9L)).thenReturn(Optional.of(session));

        EndLiveSurfSessionRequest endRequest = new EndLiveSurfSessionRequest();
        endRequest.setWaveSize(WaveSize.SMALL);
        endRequest.setSessionNotes("Fun waves");

        SurfSessionListItemDTO ended = surfSessionService.endLiveSession("u1", 9L, endRequest);

        assertEquals(SessionStatus.COMPLETED, ended.getStatus());
        assertNull(session.getWaveSize());
        assertNull(session.getSessionNotes());
        assertEquals(end, session.getSessionEndInstant());
        verify(sessionNotificationService, never()).notifySessionEnded(any(), any());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void endLiveSessionShouldRejectEndOnManualCompletedSession() {
        Instant start = Instant.now().minus(45, ChronoUnit.MINUTES);
        Instant end = Instant.now().minus(5, ChronoUnit.MINUTES);
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(start)
                .sessionEndInstant(end)
                .durationMinutes(40)
                .status(SessionStatus.COMPLETED)
                .build();
        session.setId(9L);
        when(surfSessionRepository.findByIdWithUserForUpdate(9L)).thenReturn(Optional.of(session));

        EndLiveSurfSessionRequest endRequest = new EndLiveSurfSessionRequest();
        endRequest.setWaveSize(WaveSize.SMALL);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> surfSessionService.endLiveSession("u1", 9L, endRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(ApiErrors.SURF_SESSION_END_REQUIRES_LIVE_START, exception.getReason());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void endLiveSessionShouldRejectInProgressSessionWithoutLiveStart() {
        Instant start = Instant.now().minus(30, ChronoUnit.MINUTES);
        SurfSession session = SurfSession.builder()
                .user(user)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(start)
                .status(SessionStatus.IN_PROGRESS)
                .build();
        session.setId(9L);
        when(surfSessionRepository.findByIdWithUserForUpdate(9L)).thenReturn(Optional.of(session));

        EndLiveSurfSessionRequest endRequest = new EndLiveSurfSessionRequest();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> surfSessionService.endLiveSession("u1", 9L, endRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(ApiErrors.SURF_SESSION_END_REQUIRES_LIVE_START, exception.getReason());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void endLiveSessionShouldCapDurationAt24Hours() {
        Instant start = Instant.now().minus(30, ChronoUnit.HOURS);
        SurfSession session = SurfSession.builder()
                .user(user)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(start)
                .startLatitude(54.4783)
                .startLongitude(-8.2779)
                .status(SessionStatus.IN_PROGRESS)
                .build();
        session.setId(9L);
        when(surfSessionRepository.findByIdWithUserForUpdate(9L)).thenReturn(Optional.of(session));

        EndLiveSurfSessionRequest endRequest = new EndLiveSurfSessionRequest();
        endRequest.setSessionRating(3);

        SurfSessionListItemDTO ended = surfSessionService.endLiveSession("u1", 9L, endRequest);

        assertEquals(SessionStatus.COMPLETED, ended.getStatus());
        assertEquals(24 * 60, session.getDurationMinutes());
        verify(surfSessionRepository).save(session);
    }

    @Test
    void updateSessionShouldRejectInProgressSession() {
        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .status(SessionStatus.IN_PROGRESS)
                .build();
        session.setId(11L);
        when(surfSessionRepository.findById(11L)).thenReturn(Optional.of(session));
        request.setSurfSpotId(10L);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> surfSessionService.updateSession("u1", 11L, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(ApiErrors.SURF_SESSION_IN_PROGRESS_USE_END, exception.getReason());
        verify(surfSessionRepository, never()).save(any());
    }

    @Test
    void linkSessionsToSpotShouldLinkNearbySessionsAndExplicitSession() {
        SurfSpot newSpot = SurfSpot.builder().id(22L).ianaZoneId("UTC").build();
        when(surfSpotRepository.findById(22L)).thenReturn(Optional.of(newSpot));

        SurfSession nearbySession = SurfSession.builder()
                .user(user)
                .status(SessionStatus.COMPLETED)
                .startLatitude(54.4783)
                .startLongitude(-8.2779)
                .build();
        nearbySession.setId(40L);

        SurfSession explicitFarSession = SurfSession.builder()
                .user(user)
                .status(SessionStatus.COMPLETED)
                .startLatitude(54.60)
                .startLongitude(-8.40)
                .build();
        explicitFarSession.setId(41L);

        SurfSession distantSession = SurfSession.builder()
                .user(user)
                .status(SessionStatus.COMPLETED)
                .startLatitude(55.0)
                .startLongitude(-9.0)
                .build();
        distantSession.setId(42L);

        when(surfSessionRepository.findById(41L)).thenReturn(Optional.of(explicitFarSession));
        when(surfSessionRepository.findById(40L)).thenReturn(Optional.of(nearbySession));
        when(surfSessionRepository.findUnassignedWithStartLocationByUserIdExcludingInProgress(
                        eq("u1"), eq(SessionStatus.IN_PROGRESS)))
                .thenReturn(List.of(nearbySession, explicitFarSession, distantSession));

        LinkSessionsToSpotRequest linkRequest = new LinkSessionsToSpotRequest();
        linkRequest.setSurfSpotId(22L);
        linkRequest.setAnchorLatitude(54.4783);
        linkRequest.setAnchorLongitude(-8.2779);
        linkRequest.setSessionId(41L);

        var result = surfSessionService.linkSessionsToSpot("u1", linkRequest);

        assertEquals(2, result.getLinkedSessionCount());
        assertEquals(newSpot, nearbySession.getSurfSpot());
        assertEquals(newSpot, explicitFarSession.getSurfSpot());
        assertNull(distantSession.getSurfSpot());
        verify(userSurfSpotService, times(2)).addUserSurfSpot("u1", 22L);
        verify(surfSessionRepository, times(2)).save(any(SurfSession.class));
    }

    @Test
    void linkSessionsToSpotShouldRejectForeignExplicitSession() {
        SurfSpot newSpot = SurfSpot.builder().id(22L).ianaZoneId("UTC").build();
        when(surfSpotRepository.findById(22L)).thenReturn(Optional.of(newSpot));

        User otherUser = User.builder().id("other").build();
        SurfSession foreignSession = SurfSession.builder()
                .user(otherUser)
                .status(SessionStatus.COMPLETED)
                .startLatitude(54.4783)
                .startLongitude(-8.2779)
                .build();
        foreignSession.setId(50L);
        when(surfSessionRepository.findById(50L)).thenReturn(Optional.of(foreignSession));

        LinkSessionsToSpotRequest linkRequest = new LinkSessionsToSpotRequest();
        linkRequest.setSurfSpotId(22L);
        linkRequest.setAnchorLatitude(54.4783);
        linkRequest.setAnchorLongitude(-8.2779);
        linkRequest.setSessionId(50L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> surfSessionService.linkSessionsToSpot("u1", linkRequest));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(surfSessionRepository, never()).save(any());
    }
}
