package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.config.AppProperties;
import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.NotificationEmailSent;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;
import com.lovettj.surfspotsapi.repository.NotificationEmailSentRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;

@ExtendWith(MockitoExtension.class)
class WatchListNotificationEmailServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WatchListRepository watchListRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationEmailSentRepository notificationEmailSentRepository;

    @Mock
    private EmailService emailService;

    private WatchListNotificationEmailService watchListNotificationEmailService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setUrl("https://surfspots.example");
        watchListNotificationEmailService =
                new WatchListNotificationEmailService(
                        userRepository,
                        watchListRepository,
                        notificationService,
                        notificationEmailSentRepository,
                        emailService,
                        appProperties);
    }

    @Test
    void testProcessWatchListAlertEmailsShouldSendWhenSwellOptedIn() {
        User user = buildUser(true, false);
        when(userRepository.findUsersWithWatchListEmailAlertsEnabled()).thenReturn(List.of(user));
        when(watchListRepository.findByUserId("user-1")).thenReturn(List.of(new WatchListSurfSpot()));
        when(notificationService.generateNotifications(any()))
                .thenReturn(
                        List.of(
                                NotificationDTO.builder()
                                        .id("swell-1-2026-starting")
                                        .type("swell")
                                        .title("Swell season starting")
                                        .description("Get ready")
                                        .link("/watch-list")
                                        .build()));
        when(notificationEmailSentRepository.existsByUserIdAndNotificationKey(
                        eq("user-1"), eq("swell-1-2026-starting")))
                .thenReturn(false);
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(true);

        int sentCount = watchListNotificationEmailService.processWatchListAlertEmails();

        assertEquals(1, sentCount);
        verify(emailService)
                .sendEmail(
                        eq("surfer@example.com"),
                        eq("Swell season starting"),
                        eq(TransactionalEmailTemplate.WATCH_LIST_ALERT.getLogicalName()),
                        anyMap());
        verify(notificationEmailSentRepository).saveAndFlush(any(NotificationEmailSent.class));
    }

    @Test
    void testProcessWatchListAlertEmailsShouldSkipSwellWhenOptedOut() {
        User user = buildUser(false, true);
        when(userRepository.findUsersWithWatchListEmailAlertsEnabled()).thenReturn(List.of(user));
        when(watchListRepository.findByUserId("user-1")).thenReturn(List.of(new WatchListSurfSpot()));
        when(notificationService.generateNotifications(any()))
                .thenReturn(
                        List.of(
                                NotificationDTO.builder()
                                        .id("swell-1-2026-starting")
                                        .type("swell")
                                        .title("Swell season starting")
                                        .description("Get ready")
                                        .build()));

        int sentCount = watchListNotificationEmailService.processWatchListAlertEmails();

        assertEquals(0, sentCount);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void testProcessWatchListAlertEmailsShouldNotClaimWhenSendFails() {
        User user = buildUser(true, false);
        when(userRepository.findUsersWithWatchListEmailAlertsEnabled()).thenReturn(List.of(user));
        when(watchListRepository.findByUserId("user-1")).thenReturn(List.of(new WatchListSurfSpot()));
        when(notificationService.generateNotifications(any()))
                .thenReturn(
                        List.of(
                                NotificationDTO.builder()
                                        .id("swell-1-2026-starting")
                                        .type("swell")
                                        .title("Swell season starting")
                                        .description("Get ready")
                                        .build()));
        when(notificationEmailSentRepository.existsByUserIdAndNotificationKey(anyString(), anyString()))
                .thenReturn(false);
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(false);

        int sentCount = watchListNotificationEmailService.processWatchListAlertEmails();

        assertEquals(0, sentCount);
        verify(notificationEmailSentRepository, never()).saveAndFlush(any());
    }

    private static User buildUser(boolean swellEmails, boolean eventEmails) {
        Settings settings = Settings.builder()
                .swellSeasonEmails(swellEmails)
                .eventEmails(eventEmails)
                .build();
        User user = new User();
        user.setId("user-1");
        user.setEmail("surfer@example.com");
        user.setSettings(settings);
        return user;
    }
}
