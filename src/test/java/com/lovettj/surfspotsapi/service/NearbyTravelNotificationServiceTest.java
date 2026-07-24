package com.lovettj.surfspotsapi.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.config.AppProperties;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.NotificationEmailSentRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NearbyTravelNotificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SurfSpotRepository surfSpotRepository;

    @Mock
    private NotificationEmailSentRepository notificationEmailSentRepository;

    @Mock
    private EmailService emailService;

    private NearbyTravelNotificationService nearbyTravelNotificationService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setUrl("https://surfspots.example");
        nearbyTravelNotificationService =
                new NearbyTravelNotificationService(
                        userRepository,
                        surfSpotRepository,
                        notificationEmailSentRepository,
                        emailService,
                        appProperties);
    }

    @Test
    void testReportLocationShouldStoreBaselineWithoutEmailOnFirstReport() {
        User user = buildUser(null, null, true, "metric");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        nearbyTravelNotificationService.reportLocation("user-1", 54.5, -8.2);

        assertEquals(54.5, user.getSettings().getLastKnownLatitude());
        assertEquals(-8.2, user.getSettings().getLastKnownLongitude());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void testReportLocationShouldNotStoreWhenNearbyEmailsDisabled() {
        User user = buildUser(10.0, 10.0, false, "metric");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        nearbyTravelNotificationService.reportLocation("user-1", 54.5, -8.2);

        assertNull(user.getSettings().getLastKnownLatitude());
        assertNull(user.getSettings().getLastKnownLongitude());
        verify(userRepository).save(user);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void testReportLocationShouldEmailWhenTravelJumpAndOptedIn() {
        User user = buildUser(10.0, 10.0, true, "imperial");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(notificationEmailSentRepository.existsByUserIdAndNotificationKey(anyString(), anyString()))
                .thenReturn(false);

        SurfSpot nearbySpot = new SurfSpot();
        nearbySpot.setName("Local Peak");
        nearbySpot.setLatitude(54.5);
        nearbySpot.setLongitude(-8.2);
        when(surfSpotRepository.findWithinBoundsWithFilters(any())).thenReturn(List.of(nearbySpot));
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyMap())).thenReturn(true);

        nearbyTravelNotificationService.reportLocation("user-1", 54.5, -8.2);

        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService)
                .sendEmail(
                        eq("surfer@example.com"),
                        eq("Surf spots near your new location"),
                        templateCaptor.capture(),
                        anyMap());
        assertEquals(
                TransactionalEmailTemplate.NEARBY_SURF_SPOTS.getLogicalName(),
                templateCaptor.getValue());
    }

    @Test
    void testReportLocationShouldNotStoreWhenEmailUnverified() {
        User user = buildUser(10.0, 10.0, true, "metric", false);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        nearbyTravelNotificationService.reportLocation("user-1", 54.5, -8.2);

        assertNull(user.getSettings().getLastKnownLatitude());
        assertNull(user.getSettings().getLastKnownLongitude());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyMap());
    }

    private static User buildUser(
            Double lastLatitude,
            Double lastLongitude,
            boolean nearbyEmails,
            String preferredUnits) {
        return buildUser(lastLatitude, lastLongitude, nearbyEmails, preferredUnits, true);
    }

    private static User buildUser(
            Double lastLatitude,
            Double lastLongitude,
            boolean nearbyEmails,
            String preferredUnits,
            boolean emailVerified) {
        Settings settings = Settings.builder()
                .nearbySurfSpotsEmails(nearbyEmails)
                .preferredUnits(preferredUnits)
                .lastKnownLatitude(lastLatitude)
                .lastKnownLongitude(lastLongitude)
                .build();
        User user = new User();
        user.setId("user-1");
        user.setEmail("surfer@example.com");
        user.setEmailVerified(emailVerified);
        user.setSettings(settings);
        return user;
    }
}
