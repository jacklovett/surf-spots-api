package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.constants.ContestImportConstants;
import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.SurfEvent;
import com.lovettj.surfspotsapi.entity.SurfEventContestDetail;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EventSource;
import com.lovettj.surfspotsapi.enums.EventStatus;
import com.lovettj.surfspotsapi.enums.EventType;
import com.lovettj.surfspotsapi.repository.SurfEventRepository;

@ExtendWith(MockitoExtension.class)
class EventNotificationServiceTests {

    @Mock
    private SurfEventRepository surfEventRepository;

    private EventNotificationService eventNotificationService;

    @BeforeEach
    void setUp() {
        eventNotificationService = new EventNotificationService(surfEventRepository);
    }

    @Test
    void testGenerateEventNotificationsShouldReturnNotificationForActiveWatchedEvent() {
        SurfSpot pipeline = SurfSpot.builder().id(5L).name("Pipeline").build();
        SurfEvent activeEvent = contestEvent(
                20L,
                pipeline,
                "Billabong Pro Pipeline",
                "Pipeline, Oahu, Hawaii",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(5),
                EventStatus.UPCOMING);

        Map<Long, SurfSpot> watchedSpotsById = watchedSpots(pipeline);

        when(surfEventRepository.findSeasonActiveEventsForYearAndSurfSpotIds(
                        eq(EventType.CONTEST),
                        eq(LocalDate.now().getYear()),
                        eq(Set.of(5L)),
                        eq(EventStatus.excludedFromSeasonActivity())))
                .thenReturn(List.of(activeEvent));

        List<NotificationDTO> notifications =
                eventNotificationService.generateEventNotifications(watchedSpotsById);

        assertEquals(1, notifications.size());
        NotificationDTO notification = notifications.get(0);
        assertEquals("event", notification.getType());
        assertEquals("surf-event-20", notification.getId());
        assertEquals("Pipeline", notification.getSurfSpotName());
        assertEquals("Billabong Pro Pipeline — CT waiting period open", notification.getTitle());
        assertNull(notification.getLink());
        assertEquals(LocalDate.now().minusDays(1), notification.getStartDate());
        assertEquals(LocalDate.now().plusDays(5), notification.getEndDate());
        assertEquals("UPCOMING", notification.getStatus());
    }

    @Test
    void testGenerateEventNotificationsShouldCopyImportedUrlOntoNotificationLink() {
        SurfSpot pipeline = SurfSpot.builder().id(5L).name("Pipeline").build();
        SurfEvent activeEvent = contestEvent(
                20L,
                pipeline,
                "Billabong Pro Pipeline",
                "Pipeline, Oahu, Hawaii",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(5),
                EventStatus.UPCOMING);
        activeEvent.getContestDetail().setUrl(
                "https://www.worldsurfleague.com/events/2026/ct/446/lexus-pipe-masters/main");

        when(surfEventRepository.findSeasonActiveEventsForYearAndSurfSpotIds(
                        eq(EventType.CONTEST),
                        eq(LocalDate.now().getYear()),
                        eq(Set.of(5L)),
                        eq(EventStatus.excludedFromSeasonActivity())))
                .thenReturn(List.of(activeEvent));

        List<NotificationDTO> notifications =
                eventNotificationService.generateEventNotifications(watchedSpots(pipeline));

        assertEquals(
                "https://www.worldsurfleague.com/events/2026/ct/446/lexus-pipe-masters/main",
                notifications.get(0).getLink());
    }

    @Test
    void testGenerateEventNotificationsShouldReturnEmptyWhenOutsideWaitingPeriod() {
        SurfSpot pipeline = SurfSpot.builder().id(5L).name("Pipeline").build();
        SurfEvent futureEvent = contestEvent(
                21L,
                pipeline,
                "Future Event",
                "Pipeline, Oahu, Hawaii",
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                EventStatus.UPCOMING);

        Map<Long, SurfSpot> watchedSpotsById = watchedSpots(pipeline);

        when(surfEventRepository.findSeasonActiveEventsForYearAndSurfSpotIds(
                        eq(EventType.CONTEST),
                        eq(LocalDate.now().getYear()),
                        eq(Set.of(5L)),
                        eq(EventStatus.excludedFromSeasonActivity())))
                .thenReturn(List.of(futureEvent));

        List<NotificationDTO> notifications =
                eventNotificationService.generateEventNotifications(watchedSpotsById);

        assertTrue(notifications.isEmpty());
    }

    @Test
    void testGenerateEventNotificationsShouldReturnEmptyWhenRepositoryExcludesCancelledEvents() {
        SurfSpot pipeline = SurfSpot.builder().id(5L).name("Pipeline").build();
        Map<Long, SurfSpot> watchedSpotsById = watchedSpots(pipeline);

        when(surfEventRepository.findSeasonActiveEventsForYearAndSurfSpotIds(
                        eq(EventType.CONTEST),
                        eq(LocalDate.now().getYear()),
                        eq(Set.of(5L)),
                        eq(EventStatus.excludedFromSeasonActivity())))
                .thenReturn(List.of());

        List<NotificationDTO> notifications =
                eventNotificationService.generateEventNotifications(watchedSpotsById);

        assertTrue(notifications.isEmpty());
    }

    @Test
    void testGenerateEventNotificationsShouldUseLiveTitleWhenStatusActive() {
        SurfSpot pipeline = SurfSpot.builder().id(5L).name("Pipeline").build();
        SurfEvent liveEvent = contestEvent(
                23L,
                pipeline,
                "Billabong Pro Pipeline",
                "Pipeline, Oahu, Hawaii",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(5),
                EventStatus.ACTIVE);

        Map<Long, SurfSpot> watchedSpotsById = watchedSpots(pipeline);

        when(surfEventRepository.findSeasonActiveEventsForYearAndSurfSpotIds(
                        eq(EventType.CONTEST),
                        eq(LocalDate.now().getYear()),
                        eq(Set.of(5L)),
                        eq(EventStatus.excludedFromSeasonActivity())))
                .thenReturn(List.of(liveEvent));

        List<NotificationDTO> notifications =
                eventNotificationService.generateEventNotifications(watchedSpotsById);

        assertEquals("Billabong Pro Pipeline — CT stop live", notifications.get(0).getTitle());
    }

    private Map<Long, SurfSpot> watchedSpots(SurfSpot... spots) {
        Map<Long, SurfSpot> watchedSpotsById = new LinkedHashMap<>();
        for (SurfSpot spot : spots) {
            watchedSpotsById.put(spot.getId(), spot);
        }
        return watchedSpotsById;
    }

    private SurfEvent contestEvent(
            Long id,
            SurfSpot spot,
            String name,
            String locationName,
            LocalDate startDate,
            LocalDate endDate,
            EventStatus status) {
        SurfEvent event = SurfEvent.builder()
                .id(id)
                .eventType(EventType.CONTEST)
                .name(name)
                .locationName(locationName)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .source(EventSource.CONTEST_HTML_IMPORT)
                .surfSpot(spot)
                .build();
        SurfEventContestDetail detail = SurfEventContestDetail.builder()
                .organizer(ContestImportConstants.ORGANIZER_WSL)
                .series(ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES)
                .seasonYear(LocalDate.now().getYear())
                .venueLocationKey("pipeline-oahu-hawaii")
                .build();
        event.setContestDetail(detail);
        return event;
    }
}
