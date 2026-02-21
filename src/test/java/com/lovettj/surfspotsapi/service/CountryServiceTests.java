package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryServiceTests {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ContinentRepository continentRepository;

    @InjectMocks
    private CountryService countryService;

    private Continent testContinent;
    private Country testCountry;

    @BeforeEach
    void setUp() {
        testContinent = Continent.builder().id(1L).name("Europe").build();
        testContinent.generateSlug();

        testCountry = Country.builder()
                .id(1L)
                .name("France")
                .continent(testContinent)
                .build();
        testCountry.generateSlug();
    }

    @Test
    void testGetCountryBySlugShouldReturnCountryWhenSlugExists() {
        when(countryRepository.findBySlug("france")).thenReturn(Optional.of(testCountry));

        Country result = countryService.getCountryBySlug("france");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("France", result.getName());
        verify(countryRepository).findBySlug("france");
    }

    @Test
    void testGetCountryBySlugShouldThrowWhenSlugNotFound() {
        when(countryRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                countryService.getCountryBySlug("unknown"));

        assertEquals("Country not found", thrown.getMessage());
        verify(countryRepository).findBySlug("unknown");
    }

    @Test
    void testGetCountriesByContinentShouldReturnCountriesWhenContinentExists() {
        when(continentRepository.findBySlug("europe")).thenReturn(Optional.of(testContinent));
        when(countryRepository.findByContinentOrderByNameAsc(testContinent)).thenReturn(List.of(testCountry));

        List<Country> result = countryService.getCountriesByContinent("europe");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("France", result.get(0).getName());
        verify(continentRepository).findBySlug("europe");
        verify(countryRepository).findByContinentOrderByNameAsc(testContinent);
    }

    @Test
    void testGetCountriesByContinentShouldThrowWhenContinentNotFound() {
        when(continentRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                countryService.getCountriesByContinent("unknown"));

        assertEquals("Continent not found", thrown.getMessage());
        verify(continentRepository).findBySlug("unknown");
        verify(countryRepository, never()).findByContinentOrderByNameAsc(any());
    }

    @Test
    void testGetCountriesByContinentShouldReturnEmptyListWhenContinentHasNoCountries() {
        when(continentRepository.findBySlug("antarctica")).thenReturn(Optional.of(testContinent));
        when(countryRepository.findByContinentOrderByNameAsc(testContinent)).thenReturn(List.of());

        List<Country> result = countryService.getCountriesByContinent("antarctica");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllCountriesShouldReturnAllFromRepository() {
        when(countryRepository.findAllByOrderByContinentNameAscNameAsc()).thenReturn(List.of(testCountry));

        List<Country> result = countryService.getAllCountries();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("France", result.get(0).getName());
        verify(countryRepository).findAllByOrderByContinentNameAscNameAsc();
    }

    @Test
    void testFindCountryByNameShouldReturnOptionalWhenMatchExists() {
        when(countryRepository.findByNameIgnoreCase("France")).thenReturn(Optional.of(testCountry));

        Optional<Country> result = countryService.findCountryByName("France");

        assertTrue(result.isPresent());
        assertEquals("France", result.get().getName());
        verify(countryRepository).findByNameIgnoreCase("France");
    }

    @Test
    void testFindCountryByNameShouldReturnEmptyWhenNoMatch() {
        when(countryRepository.findByNameIgnoreCase("Nowhere")).thenReturn(Optional.empty());

        Optional<Country> result = countryService.findCountryByName("Nowhere");

        assertTrue(result.isEmpty());
        verify(countryRepository).findByNameIgnoreCase("Nowhere");
    }
}
