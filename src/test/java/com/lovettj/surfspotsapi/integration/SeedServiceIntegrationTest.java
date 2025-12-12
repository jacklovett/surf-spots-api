package com.lovettj.surfspotsapi.integration;

import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SubRegionRepository;
import com.lovettj.surfspotsapi.repository.SwellSeasonRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.TripSpotRepository;
import com.lovettj.surfspotsapi.repository.UserSurfSpotRepository;
import com.lovettj.surfspotsapi.service.SeedService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to ensure seeding works from an empty database.
 * This test should catch issues like Jackson annotation mismatches before they reach production.
 * 
 * Run this test locally before deploying to catch production-like issues.
 */
@SpringBootTest
@ActiveProfiles("test")
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
    private TripSpotRepository tripSpotRepository;

    @Autowired
    private UserSurfSpotRepository userSurfSpotRepository;

    @BeforeAll
    void setUp() {
        // Clean any existing data (from previous test runs or from @PostConstruct if it ran)
        // This ensures we start with a clean slate
        // Order matters: delete in reverse order of FK dependencies
        // Only runs ONCE for all tests in this class
        // Note: @PostConstruct may have run during Spring context initialization,
        // so we clean up any data that was seeded automatically
        tripSpotRepository.deleteAll();
        userSurfSpotRepository.deleteAll();
        watchListRepository.deleteAll();
        surfSpotRepository.deleteAll();
        subRegionRepository.deleteAll();
        regionRepository.deleteAll();
        countryRepository.deleteAll();
        continentRepository.deleteAll();
        swellSeasonRepository.deleteAll();
    }

    @Test
    void testSeedingFromEmptyDatabase() {
        // Clean up any data that might have been seeded by @PostConstruct
        // (even though it should be disabled in test profile, we ensure clean state)
        tripSpotRepository.deleteAll();
        userSurfSpotRepository.deleteAll();
        watchListRepository.deleteAll();
        surfSpotRepository.deleteAll();
        subRegionRepository.deleteAll();
        regionRepository.deleteAll();
        countryRepository.deleteAll();
        continentRepository.deleteAll();
        swellSeasonRepository.deleteAll();
        
        // Verify database is empty before seeding
        assertEquals(0, continentRepository.count(), "Database should be empty before seeding");
        assertEquals(0, swellSeasonRepository.count(), "Database should be empty before seeding");
        
        // This should not throw any exceptions (like Jackson deserialization errors)
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
        // First seeding
        seedService.seedData();
        long firstSwellSeasonCount = swellSeasonRepository.count();
        long firstContinentCount = continentRepository.count();
        long firstCountryCount = countryRepository.count();

        // Second seeding should not duplicate data
        seedService.seedData();
        long secondSwellSeasonCount = swellSeasonRepository.count();
        long secondContinentCount = continentRepository.count();
        long secondCountryCount = countryRepository.count();

        assertEquals(firstSwellSeasonCount, secondSwellSeasonCount, "Second seeding should not create duplicate swell seasons");
        assertEquals(firstContinentCount, secondContinentCount, "Second seeding should not create duplicate continents");
        assertEquals(firstCountryCount, secondCountryCount, "Second seeding should not create duplicate countries");
    }
}



