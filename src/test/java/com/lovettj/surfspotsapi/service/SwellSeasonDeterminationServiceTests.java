package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.lovettj.surfspotsapi.entity.SwellSeason;
import com.lovettj.surfspotsapi.repository.SwellSeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class SwellSeasonDeterminationServiceTests {

    @Mock
    private SwellSeasonRepository swellSeasonRepository;

    private SwellSeasonDeterminationService service;

    @BeforeEach
    void setUp() {
        service = new SwellSeasonDeterminationService(swellSeasonRepository);
    }

    @Test
    void testDetermineSwellSeasonShouldReturnEmptyWhenCoordinatesAreNull() {
        Optional<SwellSeason> result = service.determineSwellSeason(null, 0.0);
        assertTrue(result.isEmpty());

        result = service.determineSwellSeason(0.0, null);
        assertTrue(result.isEmpty());

        result = service.determineSwellSeason(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnNorthAtlanticForEuropeCoordinates() {
        // Portugal - Lisbon
        SwellSeason expected = createSwellSeason("North Atlantic", "September", "April");
        when(swellSeasonRepository.findByName("North Atlantic")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(38.7223, -9.1393);
        assertTrue(result.isPresent());
        assertEquals("North Atlantic", result.get().getName());
        verify(swellSeasonRepository).findByName("North Atlantic");
    }

    @Test
    void testDetermineSwellSeasonShouldReturnNorthAtlanticForEastCoastUSCoordinates() {
        // New York
        SwellSeason expected = createSwellSeason("North Atlantic", "September", "April");
        when(swellSeasonRepository.findByName("North Atlantic")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(40.7128, -74.0060);
        assertTrue(result.isPresent());
        assertEquals("North Atlantic", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnMediterraneanForSpainCoordinates() {
        // Malaga, Spain (Mediterranean coast)
        SwellSeason expected = createSwellSeason("Mediterranean", "October", "March");
        when(swellSeasonRepository.findByName("Mediterranean")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(36.7213, -4.4214);
        assertTrue(result.isPresent());
        assertEquals("Mediterranean", result.get().getName());
        verify(swellSeasonRepository).findByName("Mediterranean");
    }

    @Test
    void testDetermineSwellSeasonShouldReturnMediterraneanForAndalusiaMediterraneanCoast() {
        // Andalusia, Spain - Mediterranean coast (not Atlantic)
        SwellSeason expected = createSwellSeason("Mediterranean", "October", "March");
        when(swellSeasonRepository.findByName("Mediterranean")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(36.5, -3.5);
        assertTrue(result.isPresent());
        assertEquals("Mediterranean", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnNorthAtlanticForAndalusiaAtlanticCoast() {
        // Andalusia, Spain - Atlantic coast (not Mediterranean)
        // This tests the edge case where a region has multiple coastlines
        SwellSeason expected = createSwellSeason("North Atlantic", "September", "April");
        when(swellSeasonRepository.findByName("North Atlantic")).thenReturn(Optional.of(expected));

        // Cadiz, Spain - Atlantic coast
        Optional<SwellSeason> result = service.determineSwellSeason(36.5270, -6.2886);
        assertTrue(result.isPresent());
        assertEquals("North Atlantic", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnNorthPacificForCaliforniaCoordinates() {
        // San Diego, California
        SwellSeason expected = createSwellSeason("North Pacific", "September", "April");
        when(swellSeasonRepository.findByName("North Pacific")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(32.7157, -117.1611);
        assertTrue(result.isPresent());
        assertEquals("North Pacific", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnNorthPacificForHawaiiCoordinates() {
        // Oahu, Hawaii
        SwellSeason expected = createSwellSeason("North Pacific", "September", "April");
        when(swellSeasonRepository.findByName("North Pacific")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(21.3099, -157.8581);
        assertTrue(result.isPresent());
        assertEquals("North Pacific", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnCaribbeanForCaribbeanCoordinates() {
        // Barbados (13.1939°N, -59.5432°W)
        SwellSeason expected = createSwellSeason("Caribbean", "June", "November");
        when(swellSeasonRepository.findByName("Caribbean")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(13.1939, -59.5432);
        assertTrue(result.isPresent(), "Result should be present for Barbados coordinates");
        assertEquals("Caribbean", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnIndianOceanForMaldivesCoordinates() {
        // Maldives
        SwellSeason expected = createSwellSeason("Indian Ocean", "March", "October");
        when(swellSeasonRepository.findByName("Indian Ocean")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(4.1755, 73.5093);
        assertTrue(result.isPresent());
        assertEquals("Indian Ocean", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnIndonesiaForIndonesiaCoordinates() {
        // Bali, Indonesia
        SwellSeason expected = createSwellSeason("Indonesia", "April", "October");
        when(swellSeasonRepository.findByName("Indonesia")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(-8.3405, 115.0920);
        assertTrue(result.isPresent());
        assertEquals("Indonesia", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnTasmanSeaForAustraliaCoordinates() {
        // Sydney, Australia
        SwellSeason expected = createSwellSeason("Tasman Sea", "March", "October");
        when(swellSeasonRepository.findByName("Tasman Sea")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(-33.8688, 151.2093);
        assertTrue(result.isPresent());
        assertEquals("Tasman Sea", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnSouthAtlanticForSouthAfricaCoordinates() {
        // Cape Town, South Africa
        SwellSeason expected = createSwellSeason("South Atlantic", "March", "October");
        when(swellSeasonRepository.findByName("South Atlantic")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(-33.9249, 18.4241);
        assertTrue(result.isPresent());
        assertEquals("South Atlantic", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnWestAfricaAtlanticForMoroccoCoordinates() {
        // Taghazout, Morocco
        SwellSeason expected = createSwellSeason("West Africa Atlantic", "April", "October");
        when(swellSeasonRepository.findByName("West Africa Atlantic")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(30.5333, -9.7000);
        assertTrue(result.isPresent());
        assertEquals("West Africa Atlantic", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnRedSeaForRedSeaCoordinates() {
        // Sharm El Sheikh, Egypt
        SwellSeason expected = createSwellSeason("Red Sea", "October", "April");
        when(swellSeasonRepository.findByName("Red Sea")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(27.9158, 34.3296);
        assertTrue(result.isPresent());
        assertEquals("Red Sea", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnNorthSeaForNorthSeaCoordinates() {
        // Amsterdam, Netherlands
        SwellSeason expected = createSwellSeason("North Sea", "September", "April");
        when(swellSeasonRepository.findByName("North Sea")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(52.3676, 4.9041);
        assertTrue(result.isPresent());
        assertEquals("North Sea", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnBalticSeaForBalticSeaCoordinates() {
        // Stockholm, Sweden
        SwellSeason expected = createSwellSeason("Baltic Sea", "October", "April");
        when(swellSeasonRepository.findByName("Baltic Sea")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(59.3293, 18.0686);
        assertTrue(result.isPresent());
        assertEquals("Baltic Sea", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnJapanNorthwestPacificForJapanCoordinates() {
        // Tokyo, Japan
        SwellSeason expected = createSwellSeason("Japan / Northwest Pacific", "August", "November");
        when(swellSeasonRepository.findByName("Japan / Northwest Pacific")).thenReturn(Optional.of(expected));

        Optional<SwellSeason> result = service.determineSwellSeason(35.6762, 139.6503);
        assertTrue(result.isPresent());
        assertEquals("Japan / Northwest Pacific", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnCentralAmericaPacificForCentralAmericaCoordinates() {
        // Costa Rica Pacific coast
        SwellSeason expected = createSwellSeason("Central America Pacific", "April", "October");
        when(swellSeasonRepository.findByName("Central America Pacific")).thenReturn(Optional.of(expected));
        // Lenient stub for Caribbean in case code path checks it (shouldn't with fixed boundaries)
        lenient().when(swellSeasonRepository.findByName("Caribbean")).thenReturn(Optional.empty());

        Optional<SwellSeason> result = service.determineSwellSeason(9.7489, -84.8381);
        assertTrue(result.isPresent());
        assertEquals("Central America Pacific", result.get().getName());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnEmptyForUnknownRegionCoordinates() {
        // Coordinates that don't match any known region (e.g., middle of ocean)
        when(swellSeasonRepository.findByName(anyString())).thenReturn(Optional.empty());

        Optional<SwellSeason> result = service.determineSwellSeason(0.0, 0.0);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDetermineSwellSeasonShouldReturnEmptyWhenRepositoryReturnsEmpty() {
        // Valid coordinates but repository doesn't find the season
        when(swellSeasonRepository.findByName("North Atlantic")).thenReturn(Optional.empty());

        Optional<SwellSeason> result = service.determineSwellSeason(40.7128, -74.0060);
        assertTrue(result.isEmpty());
    }

    // Helper method to create SwellSeason for testing
    private SwellSeason createSwellSeason(String name, String start, String end) {
        SwellSeason season = new SwellSeason();
        season.setId(1L);
        season.setName(name);
        season.setStartMonth(start);
        season.setEndMonth(end);
        return season;
    }
}

