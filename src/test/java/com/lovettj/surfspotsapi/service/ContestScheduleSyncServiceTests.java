package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.constants.ContestImportConstants;
import com.lovettj.surfspotsapi.dto.ContestScheduleImportDTO;
import com.lovettj.surfspotsapi.entity.SurfEvent;
import com.lovettj.surfspotsapi.entity.SurfEventContestDetail;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EventSource;
import com.lovettj.surfspotsapi.enums.EventStatus;
import com.lovettj.surfspotsapi.enums.EventType;
import com.lovettj.surfspotsapi.repository.SurfEventRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;

@ExtendWith(MockitoExtension.class)
class ContestScheduleSyncServiceTests {

    @Mock
    private SurfEventRepository surfEventRepository;

    @Mock
    private SurfSpotRepository surfSpotRepository;

    private ContestScheduleSyncService contestScheduleSyncService;

    @BeforeEach
    void setUp() {
        contestScheduleSyncService = new ContestScheduleSyncService(surfEventRepository, surfSpotRepository);
    }

    @Test
    void testSyncFromLocalFileShouldParseAndUpsertFromSavedHtml() throws IOException {
        Path fixturePath = Path.of("src/test/resources/contest-schedule-ct-2026-sample.html");

        when(surfEventRepository.findContestByOrganizerSeriesVenueAndSeasonYear(
                        eq(ContestImportConstants.ORGANIZER_WSL),
                        eq(ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES),
                        any(String.class),
                        eq(2026)))
                .thenReturn(Optional.empty());
        when(surfEventRepository
                        .findFirstByContestDetailOrganizerAndContestDetailSeriesAndContestDetailVenueLocationKeyAndContestDetailSeasonYearLessThanOrderByContestDetailSeasonYearDesc(
                                eq(ContestImportConstants.ORGANIZER_WSL),
                                eq(ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES),
                                any(String.class),
                                eq(2026)))
                .thenReturn(Optional.empty());
        when(surfEventRepository.save(any(SurfEvent.class))).thenAnswer(invocation -> {
            SurfEvent saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        ContestScheduleSyncService.ContestSyncResult result = contestScheduleSyncService.syncFromLocalFile(fixturePath, 2026);

        assertEquals(2026, result.year());
        assertEquals(12, result.createdCount());
    }

    @Test
    void testSyncFromLocalFileShouldThrowWhenFileMissing() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contestScheduleSyncService.syncFromLocalFile(Path.of("missing-schedule.html"), 2026));

        assertEquals("Schedule file not found: missing-schedule.html", exception.getMessage());
    }

    @Test
    void testUpsertScheduleShouldCreateEventAndAutoCarrySurfSpotFromPriorYear() {
        ContestScheduleImportDTO schedule = scheduleWithSingleEvent(
                "VIVO Rio Pro Presented By Corona Cero",
                "Saquarema, Rio de Janeiro, Brazil",
                LocalDate.of(2026, 6, 19),
                LocalDate.of(2026, 6, 27),
                "Upcoming");

        SurfSpot pipelineSpot = SurfSpot.builder().id(42L).name("Saquarema").isWslTourStop(true).build();
        SurfEvent priorYearEvent = contestEvent(
                9L,
                "saquarema-rio-de-janeiro-brazil",
                2025,
                pipelineSpot,
                EventStatus.UPCOMING);

        when(surfEventRepository.findContestByOrganizerSeriesVenueAndSeasonYear(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        "saquarema-rio-de-janeiro-brazil",
                        2026))
                .thenReturn(Optional.empty());
        when(surfEventRepository
                        .findFirstByContestDetailOrganizerAndContestDetailSeriesAndContestDetailVenueLocationKeyAndContestDetailSeasonYearLessThanOrderByContestDetailSeasonYearDesc(
                                ContestImportConstants.ORGANIZER_WSL,
                                ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                                "saquarema-rio-de-janeiro-brazil",
                                2026))
                .thenReturn(Optional.of(priorYearEvent));
        when(surfEventRepository.save(any(SurfEvent.class))).thenAnswer(invocation -> {
            SurfEvent saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        ContestScheduleSyncService.ContestSyncResult result = contestScheduleSyncService.upsertSchedule(schedule);

        assertEquals(2026, result.year());
        assertEquals(1, result.createdCount());
        assertEquals(0, result.updatedCount());
        assertEquals(1, result.autoLinkedCount());

        ArgumentCaptor<SurfEvent> savedEventCaptor = ArgumentCaptor.forClass(SurfEvent.class);
        verify(surfEventRepository).save(savedEventCaptor.capture());
        SurfEvent savedEvent = savedEventCaptor.getValue();
        assertEquals(pipelineSpot, savedEvent.getSurfSpot());
        assertEquals(EventStatus.UPCOMING, savedEvent.getStatus());
    }

    @Test
    void testUpsertScheduleShouldUpdateExistingEventWithoutOverwritingSurfSpot() {
        ContestScheduleImportDTO schedule = scheduleWithSingleEvent(
                "Updated Event Name",
                "Pipeline, Oahu, Hawaii",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 15),
                "Completed");

        SurfSpot linkedSpot = SurfSpot.builder().id(7L).name("Pipeline").build();
        SurfEvent existingEvent = contestEvent(
                3L,
                "pipeline-oahu-hawaii",
                2026,
                linkedSpot,
                EventStatus.UPCOMING);
        existingEvent.setName("Old Name");
        existingEvent.setLocationName("Pipeline, Oahu, Hawaii");
        existingEvent.setStartDate(LocalDate.of(2026, 2, 1));
        existingEvent.setEndDate(LocalDate.of(2026, 2, 10));

        when(surfEventRepository.findContestByOrganizerSeriesVenueAndSeasonYear(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        "pipeline-oahu-hawaii",
                        2026))
                .thenReturn(Optional.of(existingEvent));
        when(surfEventRepository.save(existingEvent)).thenReturn(existingEvent);

        ContestScheduleSyncService.ContestSyncResult result = contestScheduleSyncService.upsertSchedule(schedule);

        assertEquals(0, result.createdCount());
        assertEquals(1, result.updatedCount());
        assertEquals("Updated Event Name", existingEvent.getName());
        assertEquals(EventStatus.COMPLETED, existingEvent.getStatus());
        assertEquals(linkedSpot, existingEvent.getSurfSpot());
    }

    @Test
    void testUpsertScheduleShouldThrowWhenYearMissing() {
        ContestScheduleImportDTO schedule = new ContestScheduleImportDTO();
        schedule.setEvents(List.of(new ContestScheduleImportDTO.ContestScheduleEventDTO()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> contestScheduleSyncService.upsertSchedule(schedule));

        assertEquals("Contest schedule must include year", exception.getMessage());
    }

    @Test
    void testUpsertScheduleShouldThrowWhenEventsEmpty() {
        ContestScheduleImportDTO schedule = new ContestScheduleImportDTO();
        schedule.setYear(2026);
        schedule.setEvents(List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> contestScheduleSyncService.upsertSchedule(schedule));

        assertEquals("Contest schedule must include at least one event", exception.getMessage());
    }

    @Test
    void testUpsertScheduleShouldThrowWhenEventRowMissingRequiredFields() {
        ContestScheduleImportDTO.ContestScheduleEventDTO eventRow = new ContestScheduleImportDTO.ContestScheduleEventDTO();
        eventRow.setName("Incomplete Event");
        eventRow.setLocationName("Pipeline, Oahu, Hawaii");

        ContestScheduleImportDTO schedule = new ContestScheduleImportDTO();
        schedule.setYear(2026);
        schedule.setEvents(List.of(eventRow));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> contestScheduleSyncService.upsertSchedule(schedule));

        assertEquals(
                "Event row missing required fields: startDate, endDate",
                exception.getMessage());
    }

    @Test
    void testUpsertScheduleShouldThrowWhenWaitingPeriodEndBeforeStart() {
        ContestScheduleImportDTO schedule = scheduleWithSingleEvent(
                "Bad Dates Event",
                "Pipeline, Oahu, Hawaii",
                LocalDate.of(2026, 6, 20),
                LocalDate.of(2026, 6, 10),
                null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> contestScheduleSyncService.upsertSchedule(schedule));

        assertEquals(
                "endDate must be on or after startDate for event: Bad Dates Event",
                exception.getMessage());
    }

    @Test
    void testUpsertScheduleShouldSetIsWslTourStopWhenAutoCarryingUnflaggedSpot() {
        ContestScheduleImportDTO schedule = scheduleWithSingleEvent(
                "VIVO Rio Pro Presented By Corona Cero",
                "Saquarema, Rio de Janeiro, Brazil",
                LocalDate.of(2026, 6, 19),
                LocalDate.of(2026, 6, 27),
                "Upcoming");

        SurfSpot saquaremaSpot = SurfSpot.builder().id(42L).name("Saquarema").isWslTourStop(false).build();
        SurfEvent priorYearEvent = contestEvent(
                9L,
                "saquarema-rio-de-janeiro-brazil",
                2025,
                saquaremaSpot,
                EventStatus.UPCOMING);

        when(surfEventRepository.findContestByOrganizerSeriesVenueAndSeasonYear(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        "saquarema-rio-de-janeiro-brazil",
                        2026))
                .thenReturn(Optional.empty());
        when(surfEventRepository
                        .findFirstByContestDetailOrganizerAndContestDetailSeriesAndContestDetailVenueLocationKeyAndContestDetailSeasonYearLessThanOrderByContestDetailSeasonYearDesc(
                                ContestImportConstants.ORGANIZER_WSL,
                                ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                                "saquarema-rio-de-janeiro-brazil",
                                2026))
                .thenReturn(Optional.of(priorYearEvent));
        when(surfEventRepository.save(any(SurfEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contestScheduleSyncService.upsertSchedule(schedule);

        assertEquals(true, saquaremaSpot.getIsWslTourStop());
        verify(surfSpotRepository).save(saquaremaSpot);
    }

    @Test
    void testUpsertScheduleShouldMapCancelledLabelToCancelledStatus() {
        ContestScheduleImportDTO schedule = scheduleWithSingleEvent(
                "Cancelled CT Stop",
                "Pipeline, Oahu, Hawaii",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 15),
                "Cancelled");

        when(surfEventRepository.findContestByOrganizerSeriesVenueAndSeasonYear(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        "pipeline-oahu-hawaii",
                        2026))
                .thenReturn(Optional.empty());
        when(surfEventRepository
                        .findFirstByContestDetailOrganizerAndContestDetailSeriesAndContestDetailVenueLocationKeyAndContestDetailSeasonYearLessThanOrderByContestDetailSeasonYearDesc(
                                ContestImportConstants.ORGANIZER_WSL,
                                ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                                "pipeline-oahu-hawaii",
                                2026))
                .thenReturn(Optional.empty());
        when(surfEventRepository.save(any(SurfEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contestScheduleSyncService.upsertSchedule(schedule);

        ArgumentCaptor<SurfEvent> savedEventCaptor = ArgumentCaptor.forClass(SurfEvent.class);
        verify(surfEventRepository).save(savedEventCaptor.capture());
        assertEquals(EventStatus.CANCELLED, savedEventCaptor.getValue().getStatus());
    }

    private SurfEvent contestEvent(
            Long id, String venueLocationKey, int seasonYear, SurfSpot spot, EventStatus status) {
        SurfEvent event = SurfEvent.builder()
                .id(id)
                .eventType(EventType.CONTEST)
                .name("Test Event")
                .locationName("Test Location")
                .startDate(LocalDate.of(seasonYear, 3, 1))
                .endDate(LocalDate.of(seasonYear, 3, 15))
                .status(status)
                .source(EventSource.CONTEST_HTML_IMPORT)
                .surfSpot(spot)
                .build();
        SurfEventContestDetail detail = SurfEventContestDetail.builder()
                .organizer(ContestImportConstants.ORGANIZER_WSL)
                .series(ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES)
                .seasonYear(seasonYear)
                .venueLocationKey(venueLocationKey)
                .build();
        event.setContestDetail(detail);
        return event;
    }

    private ContestScheduleImportDTO scheduleWithSingleEvent(
            String name,
            String locationName,
            LocalDate startDate,
            LocalDate endDate,
            String status) {
        ContestScheduleImportDTO.ContestScheduleEventDTO eventRow = new ContestScheduleImportDTO.ContestScheduleEventDTO();
        eventRow.setName(name);
        eventRow.setLocationName(locationName);
        eventRow.setStartDate(startDate);
        eventRow.setEndDate(endDate);
        eventRow.setStatus(status);

        ContestScheduleImportDTO schedule = new ContestScheduleImportDTO();
        schedule.setYear(2026);
        schedule.setEvents(List.of(eventRow));
        return schedule;
    }
}
