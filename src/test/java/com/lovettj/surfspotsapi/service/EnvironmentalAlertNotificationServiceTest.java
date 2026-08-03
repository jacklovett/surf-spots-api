package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.EnvironmentalAlert;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertSeverity;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertStatus;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertType;
import com.lovettj.surfspotsapi.repository.EnvironmentalAlertRepository;

@ExtendWith(MockitoExtension.class)
class EnvironmentalAlertNotificationServiceTest {

    @Mock
    private EnvironmentalAlertRepository environmentalAlertRepository;

    private EnvironmentalAlertNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService =
                new EnvironmentalAlertNotificationService(environmentalAlertRepository);
    }

    @Test
    void generateHazardNotificationsShouldUseWatchedSpotNamesNotLazyAssociation() {
        SurfSpot watchedSpot = SurfSpot.builder().id(10L).name("Supertubos").build();
        SurfSpot associationStub = SurfSpot.builder().id(10L).build();
        EnvironmentalAlert alert = EnvironmentalAlert.builder()
                .id(5L)
                .surfSpot(associationStub)
                .type(EnvironmentalAlertType.SEWAGE_OVERFLOW)
                .severity(EnvironmentalAlertSeverity.WARNING)
                .title("Sewage pollution alert")
                .description("Active storm overflow into a nearby watercourse.")
                .sourceName("Scottish Water")
                .sourceUrl("https://www.scottishwater.co.uk/")
                .externalId("ext-1")
                .detectedAt(Instant.parse("2026-07-28T10:15:00Z"))
                .status(EnvironmentalAlertStatus.ACTIVE)
                .build();

        Map<Long, SurfSpot> watchedSpotsById = Map.of(10L, watchedSpot);
        when(environmentalAlertRepository.findBySurfSpotIdInAndStatusOrderByDetectedAtDesc(
                        watchedSpotsById.keySet(), EnvironmentalAlertStatus.ACTIVE))
                .thenReturn(List.of(alert));

        List<NotificationDTO> notifications =
                notificationService.generateHazardNotifications(watchedSpotsById);

        assertEquals(1, notifications.size());
        NotificationDTO notification = notifications.get(0);
        assertEquals("hazard", notification.getType());
        assertEquals("Supertubos", notification.getSurfSpotName());
        assertEquals("Sewage pollution alert", notification.getTitle());
        assertEquals("Scottish Water", notification.getLocation());
        assertEquals("WARNING", notification.getStatus());
        assertEquals("https://www.scottishwater.co.uk/", notification.getLink());
    }

    @Test
    void generateHazardNotificationsShouldReturnEmptyWhenNoWatchedSpots() {
        assertTrue(notificationService.generateHazardNotifications(Map.of()).isEmpty());
    }
}
