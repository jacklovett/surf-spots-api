package com.lovettj.surfspotsapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.constants.ContestImportConstants;
import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.dto.ContestScheduleImportDTO;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfEvent;
import com.lovettj.surfspotsapi.entity.SurfEventContestDetail;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EventSource;
import com.lovettj.surfspotsapi.enums.EventStatus;
import com.lovettj.surfspotsapi.enums.EventType;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SurfEventRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.service.ContestVenueLinkService;
import com.lovettj.surfspotsapi.service.ContestScheduleSyncService;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/surf-event-status-constraint.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class SurfEventFilterIntegrationTest {

    @Autowired
    private ContinentRepository continentRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private SurfSpotRepository surfSpotRepository;

    @Autowired
    private SurfEventRepository surfEventRepository;

    @Autowired
    private ContestScheduleSyncService contestScheduleSyncService;

    @Autowired
    private ContestVenueLinkService contestVenueLinkService;

    private Region testRegion;

    @BeforeEach
    void setUp() {
        Continent continent = continentRepository.save(Continent.builder().name("WSL Test Continent").build());
        continent.generateSlug();
        continent = continentRepository.save(continent);

        Country country = countryRepository.save(
                Country.builder().name("WSL Test Country").continent(continent).build());
        country.generateSlug();
        country = countryRepository.save(country);

        testRegion = regionRepository.save(
                Region.builder().name("WSL Test Region").country(country).build());
        testRegion.generateSlug();
        testRegion = regionRepository.save(testRegion);
    }

    @Test
    void testContestVenueFilterShouldIncludeHistoricalFlagOrCurrentSeasonLinkedSpot() {
        int currentYear = LocalDate.now().getYear();

        SurfSpot pastVenueSpot = saveSpot("Past Contest Venue", true);
        SurfSpot currentSeasonSpot = saveSpot("Current Season Venue", false);
        SurfSpot regularSpot = saveSpot("Regular Beach", false);
        SurfSpot completedOnlySpot = saveSpot("Completed Season Venue", false);

        linkCurrentSeasonEvent(
                currentSeasonSpot, "current-season-beach-test-region", currentYear, EventStatus.UPCOMING);
        linkCurrentSeasonEvent(
                completedOnlySpot, "completed-season-beach-test-region", currentYear, EventStatus.COMPLETED);

        SurfSpotFilterDTO wslTourStopFilter = new SurfSpotFilterDTO();
        wslTourStopFilter.setIsWslTourStop(true);

        List<Long> filteredSpotIds = surfSpotRepository.findByRegionWithFilters(testRegion, wslTourStopFilter).stream()
                .map(SurfSpot::getId)
                .collect(Collectors.toList());

        assertTrue(filteredSpotIds.contains(pastVenueSpot.getId()));
        assertTrue(filteredSpotIds.contains(currentSeasonSpot.getId()));
        assertFalse(filteredSpotIds.contains(regularSpot.getId()));
        assertFalse(filteredSpotIds.contains(completedOnlySpot.getId()));
        assertEquals(2, filteredSpotIds.size());
    }

    @Test
    void testCancelledEventShouldBeExcludedFromSeasonActivity() {
        int currentYear = LocalDate.now().getYear();
        SurfSpot cancelledVenueSpot = saveSpot("Cancelled CT Venue", false);

        SurfEvent cancelledEvent = SurfEvent.builder()
                .eventType(EventType.CONTEST)
                .name("Cancelled CT Stop")
                .locationName("Cancelled Beach, Test Region")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .status(EventStatus.CANCELLED)
                .source(EventSource.CONTEST_HTML_IMPORT)
                .surfSpot(cancelledVenueSpot)
                .build();
        SurfEventContestDetail detail = SurfEventContestDetail.builder()
                .organizer(ContestImportConstants.ORGANIZER_WSL)
                .series(ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES)
                .seasonYear(currentYear)
                .venueLocationKey("cancelled-beach-test-region")
                .build();
        cancelledEvent.setContestDetail(detail);
        surfEventRepository.save(cancelledEvent);

        Set<Long> activeContestSpotIds = surfEventRepository.findLinkedSurfSpotIdsForSeasonYearExcludingStatuses(
                EventType.CONTEST, currentYear, EventStatus.excludedFromSeasonActivity());
        assertFalse(activeContestSpotIds.contains(cancelledVenueSpot.getId()));
    }

    @Test
    void testUpsertAndLinkShouldIncludeSpotInContestVenueFilter() {
        int currentYear = LocalDate.now().getYear();
        SurfSpot linkedSpot = saveSpot("Sync Path Spot", false);

        ContestScheduleImportDTO.ContestScheduleEventDTO eventRow = new ContestScheduleImportDTO.ContestScheduleEventDTO();
        eventRow.setName("Sync Integration CT Stop");
        eventRow.setLocationName("Sync Path Beach, Test Region");
        eventRow.setStartDate(LocalDate.now().minusDays(1));
        eventRow.setEndDate(LocalDate.now().plusDays(4));
        eventRow.setStatus("Upcoming");

        ContestScheduleImportDTO schedule = new ContestScheduleImportDTO();
        schedule.setYear(currentYear);
        schedule.setEvents(List.of(eventRow));

        contestScheduleSyncService.upsertSchedule(schedule);
        contestVenueLinkService.linkVenueToSurfSpot("sync-path-beach-test-region", linkedSpot.getId());

        SurfSpotFilterDTO wslTourStopFilter = new SurfSpotFilterDTO();
        wslTourStopFilter.setIsWslTourStop(true);

        List<Long> filteredSpotIds = surfSpotRepository.findByRegionWithFilters(testRegion, wslTourStopFilter).stream()
                .map(SurfSpot::getId)
                .collect(Collectors.toList());

        assertTrue(filteredSpotIds.contains(linkedSpot.getId()));
    }

    private SurfSpot saveSpot(String name, boolean isWslTourStop) {
        SurfSpot spot = SurfSpot.builder()
                .name(name)
                .region(testRegion)
                .status(SurfSpotStatus.APPROVED)
                .isWslTourStop(isWslTourStop)
                .build();
        spot.generateSlug();
        return surfSpotRepository.save(spot);
    }

    private void linkCurrentSeasonEvent(
            SurfSpot spot, String venueKey, int year, EventStatus status) {
        SurfEvent event = SurfEvent.builder()
                .eventType(EventType.CONTEST)
                .name("Test CT Stop")
                .locationName("Test Location")
                .startDate(LocalDate.of(year, 3, 1))
                .endDate(LocalDate.of(year, 3, 15))
                .status(status)
                .source(EventSource.CONTEST_HTML_IMPORT)
                .surfSpot(spot)
                .build();
        SurfEventContestDetail detail = SurfEventContestDetail.builder()
                .organizer(ContestImportConstants.ORGANIZER_WSL)
                .series(ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES)
                .seasonYear(year)
                .venueLocationKey(venueKey)
                .build();
        event.setContestDetail(detail);
        surfEventRepository.save(event);
        
        if (Boolean.TRUE.equals(spot.getIsWslTourStop()) == false && status != EventStatus.COMPLETED) {
            spot.setIsWslTourStop(true);
            surfSpotRepository.save(spot);
        }
    }
}
