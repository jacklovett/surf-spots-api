package com.lovettj.surfspotsapi.integration;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to ensure seeding logic works correctly.
 * Uses test seed data files from test/resources (not production data).
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
        // First seeding
        seedService.seedData();
        long firstSwellSeasonCount = swellSeasonRepository.count();
        long firstContinentCount = continentRepository.count();
        long firstCountryCount = countryRepository.count();
        long firstRegionCount = regionRepository.count();

        // Second seeding should not duplicate data (updates existing entities instead)
        seedService.seedData();
        long secondSwellSeasonCount = swellSeasonRepository.count();
        long secondContinentCount = continentRepository.count();
        long secondCountryCount = countryRepository.count();
        long secondRegionCount = regionRepository.count();

        assertEquals(firstSwellSeasonCount, secondSwellSeasonCount, "Second seeding should not create duplicate swell seasons");
        assertEquals(firstContinentCount, secondContinentCount, "Second seeding should not create duplicate continents");
        assertEquals(firstCountryCount, secondCountryCount, "Second seeding should not create duplicate countries");
        assertEquals(firstRegionCount, secondRegionCount, "Second seeding should not create duplicate regions (dedup by country+name)");
    }

    @Test
    void testSeedingUpdatesExistingEntities() {
        // First seeding
        seedService.seedData();

        // Find an existing continent and store its original description
        Optional<Continent> continentOpt = continentRepository.findAll().stream().findFirst();
        assertTrue(continentOpt.isPresent(), "At least one continent should exist after seeding");
        
        Continent continent = continentOpt.get();
        String originalDescription = continent.getDescription();
        Long originalId = continent.getId();
        assertNotNull(originalDescription, "Continent should have a description");
        
        // Manually modify the description (simulating a change in the database)
        String modifiedDescription = "MODIFIED DESCRIPTION FOR TESTING";
        continent.setDescription(modifiedDescription);
        continentRepository.save(continent);
        continentRepository.flush();
        
        // Verify the modification was saved
        Continent modifiedContinent = continentRepository.findById(originalId).orElseThrow();
        assertEquals(modifiedDescription, modifiedContinent.getDescription(), 
                "Continent description should be modified before re-seeding");
        
        // Re-seed - this should update the description back to the original value from JSON
        seedService.seedData();
        
        // Verify the description was updated back to the original value
        Continent updatedContinent = continentRepository.findById(originalId).orElseThrow();
        assertEquals(originalDescription, updatedContinent.getDescription(), 
                "Re-seeding should update the continent description back to the value from JSON");
        assertEquals(originalId, updatedContinent.getId(), 
                "Entity ID should remain unchanged after update");
    }

    @Test
    void testSeedingUpdatesCountryWithForeignKey() {
        // First seeding
        seedService.seedData();

        // Find an existing country and store its original description and continent
        Optional<Country> countryOpt = countryRepository.findAll().stream()
                .filter(c -> c.getContinent() != null)
                .findFirst();
        assertTrue(countryOpt.isPresent(), "At least one country with a continent should exist after seeding");
        
        Country country = countryOpt.get();
        String originalDescription = country.getDescription();
        Continent originalContinent = country.getContinent();
        Long originalId = country.getId();
        Long originalContinentId = originalContinent.getId();
        
        // Manually modify the description
        String modifiedDescription = "MODIFIED COUNTRY DESCRIPTION";
        country.setDescription(modifiedDescription);
        countryRepository.save(country);
        countryRepository.flush();
        
        // Verify the modification was saved
        Country modifiedCountry = countryRepository.findById(originalId).orElseThrow();
        assertEquals(modifiedDescription, modifiedCountry.getDescription(), 
                "Country description should be modified before re-seeding");
        
        // Re-seed - this should update the description back to the original value from JSON
        seedService.seedData();
        
        // Verify the description was updated and continent relationship is preserved
        Country updatedCountry = countryRepository.findById(originalId).orElseThrow();
        assertEquals(originalDescription, updatedCountry.getDescription(), 
                "Re-seeding should update the country description back to the value from JSON");
        assertEquals(originalId, updatedCountry.getId(), 
                "Entity ID should remain unchanged after update");
        assertNotNull(updatedCountry.getContinent(), 
                "Country should still have a continent after update");
        assertEquals(originalContinentId, updatedCountry.getContinent().getId(), 
                "Continent relationship should be preserved after update");
    }

    @Test
    void testSeedRegionsShouldRemoveDuplicateRegionsAndReassignSurfSpots() {
        // Given - initial seed so we have countries and regions
        seedService.seedData();
        Region existingRegion = regionRepository.findAll().stream()
                .filter(r -> r.getCountry() != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("At least one region with country should exist after seed"));
        Long keptRegionId = existingRegion.getId();
        Country country = existingRegion.getCountry();
        String regionName = existingRegion.getName();

        // When - we introduce a duplicate region (same country, same name) and a surf spot on it
        Region duplicateRegion = Region.builder()
                .name(regionName)
                .country(country)
                .build();
        duplicateRegion.generateSlug();
        duplicateRegion = regionRepository.save(duplicateRegion);
        Long duplicateRegionId = duplicateRegion.getId();

        SurfSpot spotOnDuplicate = SurfSpot.builder()
                .name("Spot on duplicate region")
                .latitude(0.0)
                .longitude(0.0)
                .region(duplicateRegion)
                .status(SurfSpotStatus.APPROVED)
                .rating(1)
                .build();
        spotOnDuplicate.generateSlug();
        spotOnDuplicate = surfSpotRepository.save(spotOnDuplicate);

        long regionCountBeforeSecondSeed = regionRepository.findAll().stream()
                .filter(r -> r.getCountry() != null && regionName.equals(r.getName()) && country.getId().equals(r.getCountry().getId()))
                .count();
        assertEquals(2, regionCountBeforeSecondSeed, "There should be two regions (original + duplicate) before re-seed");

        // When - re-seed runs deleteDuplicateRegions then normal region seed
        seedService.seedData();

        // Then - only one region per (country, name); surf spot points to the kept region
        List<Region> regionsWithSameName = regionRepository.findAll().stream()
                .filter(r -> r.getCountry() != null && regionName.equals(r.getName()) && country.getId().equals(r.getCountry().getId()))
                .toList();
        assertEquals(1, regionsWithSameName.size(), "After seed there should be exactly one region per (country, name)");
        assertEquals(keptRegionId, regionsWithSameName.get(0).getId(), "The kept region should be the one with smallest id");

        SurfSpot spotAfter = surfSpotRepository.findById(spotOnDuplicate.getId()).orElseThrow();
        assertNotNull(spotAfter.getRegion());
        assertEquals(keptRegionId, spotAfter.getRegion().getId(),
                "Surf spot that was on the duplicate region should now point to the kept region");
    }

    @Test
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
                .rating(1)
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
