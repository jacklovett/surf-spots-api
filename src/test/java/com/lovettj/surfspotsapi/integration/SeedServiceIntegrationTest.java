package com.lovettj.surfspotsapi.integration;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SubRegionRepository;
import com.lovettj.surfspotsapi.repository.SwellSeasonRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotNoteRepository;
import com.lovettj.surfspotsapi.repository.TripSpotRepository;
import com.lovettj.surfspotsapi.repository.UserSurfSpotRepository;
import com.lovettj.surfspotsapi.service.SeedService;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to ensure seeding logic works correctly.
 * Uses test seed data files from test/resources (not production data).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.seed.enabled=true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeedServiceIntegrationTest {

    @Autowired
    private SeedService seedService;

    @Autowired
    private ContinentRepository continentRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private SubRegionRepository subRegionRepository;

    @Autowired
    private SwellSeasonRepository swellSeasonRepository;

    @Autowired
    private WatchListRepository watchListRepository;

    @Autowired
    private SurfSpotRepository surfSpotRepository;

    @Autowired
    private SurfSpotNoteRepository surfSpotNoteRepository;

    @Autowired
    private TripSpotRepository tripSpotRepository;

    @Autowired
    private UserSurfSpotRepository userSurfSpotRepository;

    @BeforeEach
    void setUp() {
        // Clean all data before each test
        tripSpotRepository.deleteAll();
        userSurfSpotRepository.deleteAll();
        watchListRepository.deleteAll();
        surfSpotNoteRepository.deleteAll();
        surfSpotRepository.deleteAll();
        subRegionRepository.deleteAll();
        regionRepository.deleteAll();
        countryRepository.deleteAll();
        continentRepository.deleteAll();
        swellSeasonRepository.deleteAll();
    }

    @Test
    void testSeedingFromEmptyDatabase() {
        // Verify database is empty before seeding
        assertEquals(0, continentRepository.count(), "Database should be empty before seeding");
        assertEquals(0, swellSeasonRepository.count(), "Database should be empty before seeding");
        
        // This should not throw any exceptions
        assertDoesNotThrow(() -> {
            seedService.seedData();
        }, "Seeding should complete without errors from empty database");

        // Verify data was seeded
        assertTrue(swellSeasonRepository.count() > 0, "Swell seasons should be seeded");
        assertTrue(continentRepository.count() > 0, "Continents should be seeded");
        assertTrue(countryRepository.count() > 0, "Countries should be seeded");
        assertTrue(regionRepository.count() > 0, "Regions should be seeded");
    }

    @Test
    void testSeedingIdempotency() {
        seedService.seedData();
        long firstSwellSeasonCount = swellSeasonRepository.count();
        long firstContinentCount = continentRepository.count();
        long firstCountryCount = countryRepository.count();
        long firstRegionCount = regionRepository.count();

        // Second run is a no-op when data already exists (empty-database-only seed)
        seedService.seedData();
        assertEquals(firstSwellSeasonCount, swellSeasonRepository.count());
        assertEquals(firstContinentCount, continentRepository.count());
        assertEquals(firstCountryCount, countryRepository.count());
        assertEquals(firstRegionCount, regionRepository.count());
    }

    @Test
    @Transactional
    void testSurfSpotCanPersistAndLoadForecastsAndWebcams() {
        seedService.seedData();
        Region region = regionRepository.findAll().stream()
                .filter(r -> r.getCountry() != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("At least one region should exist after seed"));

        SurfSpot spot = SurfSpot.builder()
                .name("Spot with forecasts and webcams")
                .latitude(0.0)
                .longitude(0.0)
                .region(region)
                .status(SurfSpotStatus.APPROVED)
                .forecasts(Arrays.asList("https://forecast.example.com/1"))
                .webcams(Arrays.asList("https://webcam.example.com/1"))
                .build();
        spot.generateSlug();
        spot = surfSpotRepository.save(spot);
        surfSpotRepository.flush();

        SurfSpot loaded = surfSpotRepository.findById(spot.getId()).orElseThrow();
        assertNotNull(loaded.getForecasts(), "Forecasts should be persisted");
        assertEquals(1, loaded.getForecasts().size());
        assertEquals("https://forecast.example.com/1", loaded.getForecasts().get(0));
        assertNotNull(loaded.getWebcams(), "Webcams should be persisted");
        assertEquals(1, loaded.getWebcams().size());
        assertEquals("https://webcam.example.com/1", loaded.getWebcams().get(0));
    }
}
