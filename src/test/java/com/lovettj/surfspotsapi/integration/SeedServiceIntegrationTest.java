package com.lovettj.surfspotsapi.integration;

import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SubRegionRepository;
import com.lovettj.surfspotsapi.repository.SwellSeasonRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.service.SeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    void setUp() {
        // Clean any existing data (from previous test runs or before conditional was added)
        // This ensures we start with a clean slate
        // Order matters: delete in reverse order of FK dependencies
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



