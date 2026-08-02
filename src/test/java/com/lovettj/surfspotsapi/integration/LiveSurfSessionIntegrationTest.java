package com.lovettj.surfspotsapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.dto.SurfSessionListItemDTO;
import com.lovettj.surfspotsapi.dto.UserSurfSessionsDTO;
import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.SessionStatus;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.WaveSize;
import com.lovettj.surfspotsapi.repository.SurfSessionRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.EndLiveSurfSessionRequest;
import com.lovettj.surfspotsapi.requests.StartLiveSurfSessionRequest;
import com.lovettj.surfspotsapi.service.LiveSessionOverdueNotificationService;
import com.lovettj.surfspotsapi.service.SurfSessionService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LiveSurfSessionIntegrationTest {

    @Autowired
    private SurfSessionService surfSessionService;

    @Autowired
    private LiveSessionOverdueNotificationService liveSessionOverdueNotificationService;

    @Autowired
    private SurfSessionRepository surfSessionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(
                User.builder()
                        .name("Live Session Tester")
                        .email("live-session-tester@example.com")
                        .emailVerified(true)
                        .skillLevel(SkillLevel.INTERMEDIATE)
                        .build());
    }

    @Test
    void startLiveSessionWithoutSpotShouldBeListedAndLoadableAsInProgress() {
        StartLiveSurfSessionRequest startRequest = new StartLiveSurfSessionRequest();
        startRequest.setStartLatitude(54.4783);
        startRequest.setStartLongitude(-8.2779);
        startRequest.setStartIanaZoneId("Europe/Dublin");

        SurfSessionListItemDTO started =
                surfSessionService.startLiveSession(testUser.getId(), startRequest);

        assertEquals(SessionStatus.IN_PROGRESS, started.getStatus());
        assertNull(started.getSurfSpotId());
        assertEquals("Live session", started.getSurfSpotName());

        SurfSession persistedStart =
                surfSessionRepository.findById(started.getId()).orElseThrow();
        assertEquals("Europe/Dublin", persistedStart.getStartIanaZoneId());

        SurfSessionListItemDTO inProgress =
                surfSessionService.getInProgressSessionForUser(testUser.getId()).orElseThrow();
        assertEquals(started.getId(), inProgress.getId());
        assertNull(inProgress.getSurfSpotId());
        assertEquals("Live session", inProgress.getSurfSpotName());

        UserSurfSessionsDTO sessionsPage =
                surfSessionService.getSurfSessionsForUser(testUser.getId());
        assertEquals(1L, sessionsPage.getTotalSessions());
        assertEquals(1, sessionsPage.getSessions().size());
        assertEquals(started.getId(), sessionsPage.getSessions().get(0).getId());
        assertEquals(SessionStatus.IN_PROGRESS, sessionsPage.getSessions().get(0).getStatus());
        assertNull(sessionsPage.getSessions().get(0).getSurfSpotId());
        assertEquals("Live session", sessionsPage.getSessions().get(0).getSurfSpotName());
    }

    @Test
    void startLiveSessionThenEndShouldCompleteSessionAndClearInProgress() {
        StartLiveSurfSessionRequest startRequest = new StartLiveSurfSessionRequest();
        startRequest.setStartLatitude(54.4783);
        startRequest.setStartLongitude(-8.2779);
        startRequest.setStartIanaZoneId("Europe/Dublin");

        SurfSessionListItemDTO started =
                surfSessionService.startLiveSession(testUser.getId(), startRequest);

        assertEquals(SessionStatus.IN_PROGRESS, started.getStatus());
        assertNotNull(started.getId());

        EndLiveSurfSessionRequest endRequest = new EndLiveSurfSessionRequest();
        endRequest.setSessionNotes("Clean waves");
        endRequest.setWaveSize(WaveSize.SMALL);

        SurfSessionListItemDTO ended =
                surfSessionService.endLiveSession(testUser.getId(), started.getId(), endRequest);

        assertEquals(SessionStatus.COMPLETED, ended.getStatus());
        assertEquals("Clean waves", ended.getSessionNotes());
        assertNotNull(ended.getSessionEndInstant());
        assertNotNull(ended.getDurationMinutes());

        SurfSession persisted =
                surfSessionRepository.findById(started.getId()).orElseThrow();
        assertEquals(SessionStatus.COMPLETED, persisted.getStatus());
        assertEquals("Clean waves", persisted.getSessionNotes());

        assertTrue(surfSessionService.getInProgressSessionForUser(testUser.getId()).isEmpty());
    }

    @Test
    void overdueNotificationShouldMarkSessionAfterExpectedReturnPasses() {
        testUser.setEmergencyContactEmail("contact@example.com");
        testUser = userRepository.save(testUser);

        StartLiveSurfSessionRequest startRequest = new StartLiveSurfSessionRequest();
        startRequest.setStartLatitude(54.4783);
        startRequest.setStartLongitude(-8.2779);
        startRequest.setShareLocationWithEmergencyContact(true);
        startRequest.setExpectedReturnInstant(Instant.now().plus(2, ChronoUnit.HOURS));

        SurfSessionListItemDTO started =
                surfSessionService.startLiveSession(testUser.getId(), startRequest);

        SurfSession session =
                surfSessionRepository.findById(started.getId()).orElseThrow();
        session.setExpectedReturnInstant(Instant.now().minus(5, ChronoUnit.MINUTES));
        surfSessionRepository.save(session);

        int sentCount = liveSessionOverdueNotificationService.processOverdueSessions();

        assertEquals(1, sentCount);

        SurfSession updatedSession =
                surfSessionRepository.findById(started.getId()).orElseThrow();
        assertNotNull(updatedSession.getOverdueNotificationSentAt());
        assertEquals(SessionStatus.IN_PROGRESS, updatedSession.getStatus());
    }
}
