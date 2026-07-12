package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.config.AppProperties;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.SessionStatus;
import com.lovettj.surfspotsapi.enums.SkillLevel;

@ExtendWith(MockitoExtension.class)
class SessionNotificationServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NearbySurfSpotResolver nearbySurfSpotResolver;

    private SessionNotificationService sessionNotificationService;

    private User user;
    private SurfSpot surfSpot;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setUrl("http://localhost:5173");
        AppProperties.Mapbox mapbox = new AppProperties.Mapbox();
        mapbox.setAccessToken("test-mapbox-token");
        appProperties.setMapbox(mapbox);
        sessionNotificationService = new SessionNotificationService(
                emailService, nearbySurfSpotResolver, appProperties);

        user = User.builder()
                .id("user-1")
                .name("Jack")
                .emergencyContactName("Jane Doe")
                .emergencyContactEmail("jane@example.com")
                .skillLevel(SkillLevel.INTERMEDIATE)
                .emailVerified(true)
                .build();

        surfSpot = SurfSpot.builder().name("Bundoran Peak").ianaZoneId("Europe/Dublin").build();
        surfSpot.setId(5L);
    }

    @Test
    void notifySessionStartedShouldSkipWhenSharingNotOptedIn() {
        SurfSession session = liveSession(false, 54.4783, -8.2779);

        sessionNotificationService.notifySessionStarted(user, session);

        verify(emailService, never()).sendEmail(any(), any(), any(), any());
    }

    @Test
    void notifySessionStartedShouldSendEmailWithMapWhenCoordinatesPresent() {
        when(nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(any(Double.class), any(Double.class)))
                .thenReturn(Optional.empty());
        SurfSession session = liveSession(true, 54.4783, -8.2779);

        sessionNotificationService.notifySessionStarted(user, session);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendEmail(
                eq("jane@example.com"),
                eq("Jack started a surf session"),
                eq(TransactionalEmailTemplate.SESSION_STARTED.getLogicalName()),
                variablesCaptor.capture());

        Map<String, Object> variables = variablesCaptor.getValue();
        assertFalse(variables.containsKey("spotName"));
        assertTrue(variables.containsKey("mapImageUrl"));
        assertTrue(variables.containsKey("mapsLink"));
        assertEquals("All times are in Jack's local time (Europe/Dublin).", variables.get("timeZoneNote"));
    }

    @Test
    void notifySessionStartedShouldIncludeNearestSpotWhenClearlyAtApprovedSpot() {
        when(nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(54.4783, -8.2779))
                .thenReturn(Optional.of("Bundoran Peak"));
        SurfSession session = liveSession(true, 54.4783, -8.2779);

        sessionNotificationService.notifySessionStarted(user, session);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendEmail(
                eq("jane@example.com"),
                eq("Jack started a surf session near Bundoran Peak"),
                eq(TransactionalEmailTemplate.SESSION_STARTED.getLogicalName()),
                variablesCaptor.capture());

        assertEquals("Bundoran Peak", variablesCaptor.getValue().get("spotName"));
    }

    @Test
    void notifySessionEndedShouldNotIncludeMapWhenStartCoordinatesPresent() {
        when(nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(any(Double.class), any(Double.class)))
                .thenReturn(Optional.empty());
        SurfSession session = completedSession(true, null);

        sessionNotificationService.notifySessionEnded(user, session);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendEmail(
                eq("jane@example.com"),
                eq("Jack's surf session has ended"),
                eq(TransactionalEmailTemplate.SESSION_ENDED.getLogicalName()),
                variablesCaptor.capture());

        Map<String, Object> variables = variablesCaptor.getValue();
        assertFalse(variables.containsKey("mapImageUrl"));
        assertFalse(variables.containsKey("mapsLink"));
        assertFalse(variables.containsKey("spotName"));
    }

    @Test
    void notifySessionEndedShouldIncludeSpotNameWhenSpotAssignedAtEnd() {
        SurfSession session = completedSession(true, surfSpot);

        sessionNotificationService.notifySessionEnded(user, session);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendEmail(
                eq("jane@example.com"),
                eq("Jack's surf session has ended"),
                eq(TransactionalEmailTemplate.SESSION_ENDED.getLogicalName()),
                variablesCaptor.capture());

        assertEquals("Bundoran Peak", variablesCaptor.getValue().get("spotName"));
        verify(nearbySurfSpotResolver, never()).findApprovedSpotNameNearCoordinates(any(Double.class), any(Double.class));
    }

    @Test
    void notifySessionEndedShouldInferNearestSpotWhenNotAssignedAtEnd() {
        when(nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(54.4783, -8.2779))
                .thenReturn(Optional.of("Bundoran Peak"));
        SurfSession session = completedSession(true, null);

        sessionNotificationService.notifySessionEnded(user, session);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendEmail(
                eq("jane@example.com"),
                eq("Jack's surf session has ended"),
                eq(TransactionalEmailTemplate.SESSION_ENDED.getLogicalName()),
                variablesCaptor.capture());

        assertEquals("Bundoran Peak", variablesCaptor.getValue().get("spotName"));
    }

    @Test
    void notifySessionEndedShouldSendEmailWhenSharingWasOptedIn() {
        when(nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(any(Double.class), any(Double.class)))
                .thenReturn(Optional.empty());
        SurfSession session = completedSession(true, null);

        sessionNotificationService.notifySessionEnded(user, session);

        verify(emailService).sendEmail(
                eq("jane@example.com"),
                eq("Jack's surf session has ended"),
                eq(TransactionalEmailTemplate.SESSION_ENDED.getLogicalName()),
                any());
    }

    @Test
    void notifySessionOverdueShouldSendEmailWithExpectedReturnTime() {
        when(nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(any(Double.class), any(Double.class)))
                .thenReturn(Optional.empty());
        SurfSession session = liveSession(true, 54.4783, -8.2779);
        session.setExpectedReturnInstant(Instant.parse("2026-07-01T09:00:00Z"));

        sessionNotificationService.notifySessionOverdue(user, session);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendEmail(
                eq("jane@example.com"),
                eq("Jack has not ended their surf session yet"),
                eq(TransactionalEmailTemplate.SESSION_OVERDUE.getLogicalName()),
                variablesCaptor.capture());

        Map<String, Object> variables = variablesCaptor.getValue();
        assertEquals("Wed 1 Jul 2026 at 10:00", variables.get("expectedReturnTime"));
        assertEquals("All times are in Jack's local time (Europe/Dublin).", variables.get("timeZoneNote"));
        assertFalse(variables.containsKey("spotName"));
        assertTrue(variables.containsKey("mapImageUrl"));
    }

    @Test
    void notifySessionOverdueShouldSkipWhenSharingNotOptedIn() {
        SurfSession session = liveSession(false, 54.4783, -8.2779);
        session.setExpectedReturnInstant(Instant.parse("2026-07-01T09:00:00Z"));

        sessionNotificationService.notifySessionOverdue(user, session);

        verify(emailService, never()).sendEmail(any(), any(), any(), any());
    }

    private SurfSession liveSession(boolean shareLocation, Double latitude, Double longitude) {
        return SurfSession.builder()
                .user(user)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .sessionStartInstant(Instant.parse("2026-07-01T06:00:00Z"))
                .status(SessionStatus.IN_PROGRESS)
                .shareLocationWithEmergencyContact(shareLocation)
                .startLatitude(latitude)
                .startLongitude(longitude)
                .startIanaZoneId("Europe/Dublin")
                .build();
    }

    private SurfSession completedSession(boolean shareLocation, SurfSpot spot) {
        SurfSession session = liveSession(shareLocation, 54.4783, -8.2779);
        session.setSurfSpot(spot);
        session.setStatus(SessionStatus.COMPLETED);
        session.setSessionEndInstant(Instant.parse("2026-07-01T08:30:00Z"));
        session.setDurationMinutes(150);
        return session;
    }
}
