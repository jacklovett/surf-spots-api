package com.lovettj.surfspotsapi.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.NotificationEmailSent;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.repository.NotificationEmailSentRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NewSurfSpotEmailServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationEmailSentRepository notificationEmailSentRepository;

    @Mock
    private EmailService emailService;

    private NewSurfSpotEmailService newSurfSpotEmailService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setUrl("https://surfspots.example");
        AppProperties.Mapbox mapbox = new AppProperties.Mapbox();
        mapbox.setAccessToken("test-mapbox-token");
        appProperties.setMapbox(mapbox);
        newSurfSpotEmailService =
                new NewSurfSpotEmailService(
                        userRepository,
                        notificationEmailSentRepository,
                        emailService,
                        appProperties);
    }

    @Test
    void testNotifySubscribersIfApprovedShouldSkipWhenAlreadyApproved() {
        SurfSpot spot = buildSpot();
        spot.setStatus(SurfSpotStatus.APPROVED);

        newSurfSpotEmailService.notifySubscribersIfApproved(spot, SurfSpotStatus.APPROVED);

        verify(userRepository, never()).findUsersWithNewSurfSpotEmailsEnabled();
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void testNotifySubscribersIfApprovedShouldSkipCreateAsApproved() {
        SurfSpot spot = buildSpot();
        spot.setStatus(SurfSpotStatus.APPROVED);

        newSurfSpotEmailService.notifySubscribersIfApproved(spot, null);

        verify(userRepository, never()).findUsersWithNewSurfSpotEmailsEnabled();
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void testNotifySubscribersIfApprovedShouldEmailOptedInUsersOnPendingToApproved() {
        SurfSpot spot = buildSpot();
        spot.setStatus(SurfSpotStatus.APPROVED);

        User user = new User();
        user.setId("user-1");
        user.setEmail("surfer@example.com");
        when(userRepository.findUsersWithNewSurfSpotEmailsEnabled()).thenReturn(List.of(user));
        when(notificationEmailSentRepository.existsByUserIdAndNotificationKey(
                        eq("user-1"), eq("new-surf-spot-42-user-1")))
                .thenReturn(false);
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(true);

        newSurfSpotEmailService.notifySubscribersIfApproved(spot, SurfSpotStatus.PENDING);

        verify(emailService)
                .sendEmail(
                        eq("surfer@example.com"),
                        eq("New surf spot: Bundoran Peak"),
                        eq(TransactionalEmailTemplate.NEW_SURF_SPOT.getLogicalName()),
                        argThat(
                                variables ->
                                        "Bundoran Peak".equals(variables.get("spotName"))
                                                && variables.get("mapImageUrl") != null
                                                && variables
                                                        .get("mapImageUrl")
                                                        .toString()
                                                        .contains("mapbox.com")));
        verify(notificationEmailSentRepository).saveAndFlush(any(NotificationEmailSent.class));
    }

    @Test
    void testNotifySubscribersIfApprovedShouldNotClaimWhenSendFails() {
        SurfSpot spot = buildSpot();
        spot.setStatus(SurfSpotStatus.APPROVED);

        User user = new User();
        user.setId("user-1");
        user.setEmail("surfer@example.com");
        when(userRepository.findUsersWithNewSurfSpotEmailsEnabled()).thenReturn(List.of(user));
        when(notificationEmailSentRepository.existsByUserIdAndNotificationKey(anyString(), anyString()))
                .thenReturn(false);
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(false);

        newSurfSpotEmailService.notifySubscribersIfApproved(spot, SurfSpotStatus.PENDING);

        verify(notificationEmailSentRepository, never()).saveAndFlush(any());
    }

    private static SurfSpot buildSpot() {
        Continent continent = new Continent();
        continent.setSlug("europe");
        Country country = new Country();
        country.setSlug("ireland");
        country.setName("Ireland");
        country.setContinent(continent);
        Region region = new Region();
        region.setSlug("donegal");
        region.setName("Donegal");
        region.setCountry(country);

        SurfSpot spot = new SurfSpot();
        spot.setId(42L);
        spot.setName("Bundoran Peak");
        spot.setSlug("bundoran-peak");
        spot.setRegion(region);
        spot.setLatitude(54.4783);
        spot.setLongitude(-8.2779);
        return spot;
    }
}
