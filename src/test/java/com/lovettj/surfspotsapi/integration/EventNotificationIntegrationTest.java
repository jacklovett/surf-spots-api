package com.lovettj.surfspotsapi.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.constants.ContestImportConstants;
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
import com.lovettj.surfspotsapi.service.EventNotificationService;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/surf-event-status-constraint.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class EventNotificationIntegrationTest {

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
    private EventNotificationService eventNotificationService;

    private Region testRegion;

    @BeforeEach
    void setUp() {
        Continent continent = continentRepository.save(Continent.builder().name("Notification Test Continent").build());
        continent.generateSlug();
        continent = continentRepository.save(continent);

        Country country = countryRepository.save(
                Country.builder().name("Notification Test Country").continent(continent).build());
        country.generateSlug();
        country = countryRepository.save(country);

        testRegion = regionRepository.save(
                Region.builder().name("Notification Test Region").country(country).build());
        testRegion.generateSlug();
        testRegion = regionRepository.save(testRegion);
    }

    @Test
    void testCancelledEventShouldNotGenerateWatchlistNotification() {
        int currentYear = LocalDate.now().getYear();
        SurfSpot cancelledVenueSpot = saveSpot("Cancelled CT Venue");

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

        Map<Long, SurfSpot> watchedSpotsById = Map.of(cancelledVenueSpot.getId(), cancelledVenueSpot);

        assertTrue(eventNotificationService.generateEventNotifications(watchedSpotsById).isEmpty());
    }

    private SurfSpot saveSpot(String name) {
        SurfSpot spot = SurfSpot.builder()
                .name(name)
                .region(testRegion)
                .status(SurfSpotStatus.APPROVED)
                .build();
        spot.generateSlug();
        return surfSpotRepository.save(spot);
    }
}
