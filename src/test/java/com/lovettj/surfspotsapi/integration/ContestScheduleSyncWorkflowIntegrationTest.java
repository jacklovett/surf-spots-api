package com.lovettj.surfspotsapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.constants.ContestImportConstants;
import com.lovettj.surfspotsapi.dto.ContestScheduleImportDTO;
import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EventStatus;
import com.lovettj.surfspotsapi.enums.EventType;
import com.lovettj.surfspotsapi.repository.SurfEventRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.service.EventNotificationService;
import com.lovettj.surfspotsapi.service.ContestVenueLinkService;
import com.lovettj.surfspotsapi.service.ContestScheduleSyncService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContestScheduleSyncWorkflowIntegrationTest {

    @Autowired
    private ContestScheduleSyncService contestScheduleSyncService;

    @Autowired
    private ContestVenueLinkService contestVenueLinkService;

    @Autowired
    private SurfEventRepository surfEventRepository;

    @Autowired
    private SurfSpotRepository surfSpotRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    private SurfSpot testSpot;

    @BeforeEach
    void setUp() {
        testSpot = surfSpotRepository.save(SurfSpot.builder().name("Integration Test Spot").build());
    }

    @Test
    void testSyncLinkAndNotifyWorkflowShouldPersistEventsAndGenerateNotification() {
        int currentYear = LocalDate.now().getYear();

        ContestScheduleImportDTO.ContestScheduleEventDTO eventRow = new ContestScheduleImportDTO.ContestScheduleEventDTO();
        eventRow.setName("Integration CT Stop");
        eventRow.setLocationName("Integration Beach, Test Country");
        eventRow.setStartDate(LocalDate.now().minusDays(1));
        eventRow.setEndDate(LocalDate.now().plusDays(5));
        eventRow.setStatus("Upcoming");
        eventRow.setUrl("https://www.worldsurfleague.com/events/2026/ct/999/integration-ct-stop/main");

        ContestScheduleImportDTO schedule = new ContestScheduleImportDTO();
        schedule.setYear(currentYear);
        schedule.setEvents(List.of(eventRow));

        ContestScheduleSyncService.ContestSyncResult syncResult = contestScheduleSyncService.upsertSchedule(schedule);
        assertEquals(1, syncResult.createdCount());

        ContestVenueLinkService.ContestLinkResult linkResult = contestVenueLinkService.linkVenueToSurfSpot(
                "integration-beach-test-country", testSpot.getId());
        assertEquals(1, linkResult.eventsUpdated());

        SurfSpot reloadedSpot = surfSpotRepository.findById(testSpot.getId()).orElseThrow();
        assertTrue(reloadedSpot.getIsWslTourStop());

        Set<Long> activeContestSpotIds = surfEventRepository.findLinkedSurfSpotIdsForSeasonYearExcludingStatuses(
                EventType.CONTEST, currentYear, EventStatus.excludedFromSeasonActivity());
        assertTrue(activeContestSpotIds.contains(testSpot.getId()));

        Map<Long, SurfSpot> watchedSpotsById = Map.of(reloadedSpot.getId(), reloadedSpot);
        List<NotificationDTO> notifications =
                eventNotificationService.generateEventNotifications(watchedSpotsById);
        assertFalse(notifications.isEmpty());
        assertEquals(
                "https://www.worldsurfleague.com/events/2026/ct/999/integration-ct-stop/main",
                notifications.get(0).getLink());

        var persistedEvent = surfEventRepository
                .findContestByOrganizerSeriesVenueAndSeasonYear(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        "integration-beach-test-country",
                        currentYear)
                .orElseThrow();
        assertEquals(testSpot.getId(), persistedEvent.getSurfSpot().getId());
    }
}
