package com.lovettj.surfspotsapi.integration;

import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.service.SeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to ensure seeding works from an empty database.
 * This test should catch issues like Jackson annotation mismatches before they reach production.
 * 
 * Run this test locally before deploying to catch production-like issues.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeedServiceIntegrationTest {

    @Autowired
    private SeedService seedService;

    @Autowired
    private ContinentRepository continentRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private RegionRepository regionRepository;

    @BeforeEach
    void setUp() {
        // Ensure clean state - this should be handled by @Transactional rollback
        // but explicitly clearing helps ensure we're testing from empty state
    }

    @Test
    void testSeedingFromEmptyDatabase() {
        // Verify database is empty before seeding
        assertEquals(0, continentRepository.count(), "Database should be empty before seeding");
        
        // This should not throw any exceptions (like Jackson deserialization errors)
        assertDoesNotThrow(() -> {
            seedService.seedData();
        }, "Seeding should complete without errors from empty database");

        // Verify data was seeded
        assertTrue(continentRepository.count() > 0, "Continents should be seeded");
        assertTrue(countryRepository.count() > 0, "Countries should be seeded");
        assertTrue(regionRepository.count() > 0, "Regions should be seeded");
    }

    @Test
    void testSeedingIdempotency() {
        // First seeding
        seedService.seedData();
        long firstContinentCount = continentRepository.count();
        long firstCountryCount = countryRepository.count();

        // Second seeding should not duplicate data
        seedService.seedData();
        long secondContinentCount = continentRepository.count();
        long secondCountryCount = countryRepository.count();

        assertEquals(firstContinentCount, secondContinentCount, "Second seeding should not create duplicate continents");
        assertEquals(firstCountryCount, secondCountryCount, "Second seeding should not create duplicate countries");
    }
}



