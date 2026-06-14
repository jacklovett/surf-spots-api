package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.constants.ContestImportConstants;
import com.lovettj.surfspotsapi.entity.SurfEvent;
import com.lovettj.surfspotsapi.entity.SurfEventContestDetail;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EventSource;
import com.lovettj.surfspotsapi.enums.EventStatus;
import com.lovettj.surfspotsapi.enums.EventType;
import com.lovettj.surfspotsapi.repository.SurfEventRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;

@ExtendWith(MockitoExtension.class)
class ContestVenueLinkServiceTests {

    @Mock
    private SurfEventRepository surfEventRepository;

    @Mock
    private SurfSpotRepository surfSpotRepository;

    private ContestVenueLinkService contestVenueLinkService;

    @BeforeEach
    void setUp() {
        contestVenueLinkService = new ContestVenueLinkService(surfEventRepository, surfSpotRepository);
    }

    @Test
    void testLinkVenueToSurfSpotShouldUpdateAllEventsAndSetHistoricalFlag() {
        SurfSpot surfSpot = SurfSpot.builder().id(10L).name("Pipeline").isWslTourStop(false).build();
        SurfEvent event2025 = contestEvent(1L, "pipeline-oahu-hawaii", 2025);
        SurfEvent event2026 = contestEvent(2L, "pipeline-oahu-hawaii", 2026);

        when(surfSpotRepository.findById(10L)).thenReturn(java.util.Optional.of(surfSpot));
        when(surfEventRepository.findContestsByOrganizerSeriesAndVenue(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        "pipeline-oahu-hawaii"))
                .thenReturn(List.of(event2025, event2026));

        ContestVenueLinkService.ContestLinkResult result =
                contestVenueLinkService.linkVenueToSurfSpot("pipeline-oahu-hawaii", 10L);

        assertEquals(2, result.eventsUpdated());
        assertEquals("pipeline-oahu-hawaii", result.venueLocationKey());
        assertEquals(surfSpot, event2025.getSurfSpot());
        assertEquals(surfSpot, event2026.getSurfSpot());
        assertEquals(true, surfSpot.getIsWslTourStop());
        verify(surfEventRepository).saveAll(List.of(event2025, event2026));
        verify(surfSpotRepository).save(surfSpot);
    }

    @Test
    void testLinkVenueToSurfSpotShouldNormalizeLocationNameToVenueKey() {
        SurfSpot surfSpot = SurfSpot.builder().id(10L).name("Pipeline").isWslTourStop(false).build();
        SurfEvent event2026 = contestEvent(2L, "pipeline-oahu-hawaii", 2026);

        when(surfSpotRepository.findById(10L)).thenReturn(java.util.Optional.of(surfSpot));
        when(surfEventRepository.findContestsByOrganizerSeriesAndVenue(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        "pipeline-oahu-hawaii"))
                .thenReturn(List.of(event2026));

        ContestVenueLinkService.ContestLinkResult result =
                contestVenueLinkService.linkVenueToSurfSpot("Pipeline, Oahu, Hawaii", 10L);

        assertEquals("pipeline-oahu-hawaii", result.venueLocationKey());
        verify(surfEventRepository)
                .findContestsByOrganizerSeriesAndVenue(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        "pipeline-oahu-hawaii");
    }

    @Test
    void testLinkVenueToSurfSpotShouldThrowWhenVenueMissing() {
        when(surfSpotRepository.findById(99L)).thenReturn(java.util.Optional.of(SurfSpot.builder().id(99L).build()));
        when(surfEventRepository.findContestsByOrganizerSeriesAndVenue(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        "missing-venue"))
                .thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contestVenueLinkService.linkVenueToSurfSpot("missing-venue", 99L));

        assertEquals("No contest events found for venue key: missing-venue", exception.getMessage());
    }

    @Test
    void testLinkVenueToSurfSpotShouldThrowWhenSurfSpotMissing() {
        when(surfSpotRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contestVenueLinkService.linkVenueToSurfSpot("pipeline-oahu-hawaii", 99L));

        assertEquals("Surf spot not found: 99", exception.getMessage());
    }

    @Test
    void testLinkVenueToSurfSpotShouldThrowWhenVenueKeyBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contestVenueLinkService.linkVenueToSurfSpot("  ", 1L));

        assertEquals("venueLocationKey is required", exception.getMessage());
    }

    private SurfEvent contestEvent(Long id, String venueLocationKey, int seasonYear) {
        SurfEvent event = SurfEvent.builder()
                .id(id)
                .eventType(EventType.CONTEST)
                .name("Test CT Stop")
                .locationName("Test Location")
                .startDate(LocalDate.of(seasonYear, 3, 1))
                .endDate(LocalDate.of(seasonYear, 3, 15))
                .status(EventStatus.UPCOMING)
                .source(EventSource.CONTEST_HTML_IMPORT)
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
}
