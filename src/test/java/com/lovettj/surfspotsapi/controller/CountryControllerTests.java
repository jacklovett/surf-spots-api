package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.CountryEmergencyNumber;
import com.lovettj.surfspotsapi.service.CountryService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryControllerTests {

    @Mock
    private CountryService countryService;

    @InjectMocks
    private CountryController countryController;

    private Country testCountry;
    private List<CountryEmergencyNumber> emergencyNumbers;

    @BeforeEach
    void setUp() {
        Continent continent = Continent.builder().id(1L).name("Europe").build();
        continent.generateSlug();

        emergencyNumbers = new ArrayList<>();
        emergencyNumbers.add(CountryEmergencyNumber.builder()
                .id(1L)
                .label("Police")
                .number("112")
                .build());

        testCountry = Country.builder()
                .id(1L)
                .name("France")
                .continent(continent)
                .emergencyNumbers(emergencyNumbers)
                .build();
        testCountry.generateSlug();
    }

    @Test
    void testGetCountryBySlugShouldReturnOkWithCountryAndEmergencyNumbers() {
        when(countryService.getCountryBySlug("france")).thenReturn(testCountry);

        ResponseEntity<Country> response = countryController.getCountryBySlug("france");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("France", response.getBody().getName());
        assertNotNull(response.getBody().getEmergencyNumbers());
        assertEquals(1, response.getBody().getEmergencyNumbers().size());
        assertEquals("112", response.getBody().getEmergencyNumbers().get(0).getNumber());
        assertEquals("Police", response.getBody().getEmergencyNumbers().get(0).getLabel());
    }

    @Test
    void testGetCountriesByContinentShouldReturnOkWithCountries() {
        when(countryService.getCountriesByContinent("europe")).thenReturn(List.of(testCountry));

        ResponseEntity<List<Country>> response = countryController.getCountriesByContinent("europe");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertNotNull(response.getBody().get(0).getEmergencyNumbers());
    }

    @Test
    void testGetCountryBySlugShouldThrowWhenCountryNotFound() {
        when(countryService.getCountryBySlug("unknown")).thenThrow(new EntityNotFoundException("Country not found"));

        assertThrows(EntityNotFoundException.class, () ->
                countryController.getCountryBySlug("unknown"));
    }

    @Test
    void testGetCountriesByContinentShouldThrowWhenContinentNotFound() {
        when(countryService.getCountriesByContinent("unknown")).thenThrow(new EntityNotFoundException("Continent not found"));

        assertThrows(EntityNotFoundException.class, () ->
                countryController.getCountriesByContinent("unknown"));
    }

    @Test
    void testGetCountryBySlugShouldReturnOkWhenCountryHasNoEmergencyNumbers() {
        Country countryNoEmergency = Country.builder()
                .id(2L)
                .name("Germany")
                .continent(testCountry.getContinent())
                .emergencyNumbers(new ArrayList<>())
                .build();
        countryNoEmergency.generateSlug();
        when(countryService.getCountryBySlug("germany")).thenReturn(countryNoEmergency);

        ResponseEntity<Country> response = countryController.getCountryBySlug("germany");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Germany", response.getBody().getName());
        assertNotNull(response.getBody().getEmergencyNumbers());
        assertTrue(response.getBody().getEmergencyNumbers().isEmpty());
    }

    @Test
    void testGetCountriesByContinentShouldReturnOkWithEmptyListWhenNoCountries() {
        when(countryService.getCountriesByContinent("antarctica")).thenReturn(Collections.emptyList());

        ResponseEntity<List<Country>> response = countryController.getCountriesByContinent("antarctica");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}
