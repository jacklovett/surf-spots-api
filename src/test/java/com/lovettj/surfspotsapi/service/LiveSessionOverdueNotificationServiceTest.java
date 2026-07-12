package com.lovettj.surfspotsapi.service;



import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;



import java.time.Instant;

import java.time.LocalDate;

import java.util.List;

import java.util.Optional;



import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;



import com.lovettj.surfspotsapi.entity.SurfSession;

import com.lovettj.surfspotsapi.entity.User;

import com.lovettj.surfspotsapi.enums.SessionStatus;

import com.lovettj.surfspotsapi.enums.SkillLevel;

import com.lovettj.surfspotsapi.repository.SurfSessionRepository;



@ExtendWith(MockitoExtension.class)

class LiveSessionOverdueNotificationServiceTest {



    @Mock

    private SurfSessionRepository surfSessionRepository;



    @Mock

    private SessionNotificationService sessionNotificationService;



    private LiveSessionOverdueNotificationService liveSessionOverdueNotificationService;



    private User user;

    private SurfSession overdueSession;



    @BeforeEach

    void setUp() {

        liveSessionOverdueNotificationService =

                new LiveSessionOverdueNotificationService(

                        surfSessionRepository, sessionNotificationService);



        user = User.builder()

                .id("user-1")

                .name("Jack")

                .emergencyContactEmail("jane@example.com")

                .skillLevel(SkillLevel.INTERMEDIATE)

                .emailVerified(true)

                .build();



        overdueSession = SurfSession.builder()

                .user(user)

                .skillLevel(SkillLevel.INTERMEDIATE)

                .sessionDate(LocalDate.of(2026, 7, 1))

                .sessionStartInstant(Instant.parse("2026-07-01T06:00:00Z"))

                .expectedReturnInstant(Instant.parse("2026-07-01T08:00:00Z"))

                .status(SessionStatus.IN_PROGRESS)

                .shareLocationWithEmergencyContact(true)

                .startLatitude(54.4783)

                .startLongitude(-8.2779)

                .build();

        overdueSession.setId(42L);

    }



    @Test

    void processOverdueSessionsShouldSendEmailAndMarkSessionNotified() {

        when(surfSessionRepository.findOverdueLiveSessionIdsNeedingNotification(

                        eq(SessionStatus.IN_PROGRESS), any(Instant.class)))

                .thenReturn(List.of(42L));

        when(surfSessionRepository.findByIdWithUserForUpdate(42L))

                .thenReturn(Optional.of(overdueSession));

        when(surfSessionRepository.save(overdueSession)).thenReturn(overdueSession);



        int sentCount = liveSessionOverdueNotificationService.processOverdueSessions();



        assertEquals(1, sentCount);

        verify(sessionNotificationService).notifySessionOverdue(user, overdueSession);

        ArgumentCaptor<SurfSession> sessionCaptor = ArgumentCaptor.forClass(SurfSession.class);

        verify(surfSessionRepository).save(sessionCaptor.capture());

        assertNotNull(sessionCaptor.getValue().getOverdueNotificationSentAt());

    }



    @Test

    void processOverdueSessionsShouldMarkSkippedWhenEmergencyContactEmailMissing() {

        user.setEmergencyContactEmail(null);

        when(surfSessionRepository.findOverdueLiveSessionIdsNeedingNotification(

                        eq(SessionStatus.IN_PROGRESS), any(Instant.class)))

                .thenReturn(List.of(42L));

        when(surfSessionRepository.findByIdWithUserForUpdate(42L))

                .thenReturn(Optional.of(overdueSession));

        when(surfSessionRepository.save(overdueSession)).thenReturn(overdueSession);



        int sentCount = liveSessionOverdueNotificationService.processOverdueSessions();



        assertEquals(0, sentCount);

        verify(sessionNotificationService, never()).notifySessionOverdue(any(), any());

        ArgumentCaptor<SurfSession> sessionCaptor = ArgumentCaptor.forClass(SurfSession.class);

        verify(surfSessionRepository).save(sessionCaptor.capture());

        assertNotNull(sessionCaptor.getValue().getOverdueNotificationSentAt());

    }



    @Test

    void processOverdueSessionByIdShouldSkipWhenAlreadyNotified() {

        overdueSession.setOverdueNotificationSentAt(Instant.parse("2026-07-01T08:30:00Z"));

        when(surfSessionRepository.findByIdWithUserForUpdate(42L))

                .thenReturn(Optional.of(overdueSession));



        boolean sent = liveSessionOverdueNotificationService.processOverdueSessionById(

                42L, Instant.parse("2026-07-01T09:00:00Z"));



        assertFalse(sent);

        verify(sessionNotificationService, never()).notifySessionOverdue(any(), any());

        verify(surfSessionRepository, never()).save(any());

    }



    @Test

    void sendOverdueNotificationForSessionShouldNotMarkSentWhenEmailFails() {

        org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))

                .when(sessionNotificationService)

                .notifySessionOverdue(user, overdueSession);



        boolean sent =

                liveSessionOverdueNotificationService.sendOverdueNotificationForSession(

                        overdueSession, Instant.parse("2026-07-01T09:00:00Z"));



        assertFalse(sent);

        verify(surfSessionRepository, never()).save(any());

        assertEquals(null, overdueSession.getOverdueNotificationSentAt());

    }



    @Test

    void sendOverdueNotificationForSessionShouldMarkSentWhenEmailSucceeds() {

        when(surfSessionRepository.save(overdueSession)).thenReturn(overdueSession);



        boolean sent =

                liveSessionOverdueNotificationService.sendOverdueNotificationForSession(

                        overdueSession, Instant.parse("2026-07-01T09:00:00Z"));



        assertTrue(sent);

        verify(sessionNotificationService).notifySessionOverdue(user, overdueSession);

        assertNotNull(overdueSession.getOverdueNotificationSentAt());

    }

}


