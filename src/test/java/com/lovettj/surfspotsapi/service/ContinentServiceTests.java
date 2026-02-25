package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.ContinentSummaryDTO;
import com.lovettj.surfspotsapi.dto.CountrySummaryDTO;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.repository.ContinentRepository;
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
class ContinentServiceTests {

  @Mock
  private ContinentRepository continentRepository;

  @InjectMocks
  private ContinentService continentService;

  private Continent testContinent;
  private Country testCountry;

  @BeforeEach
  void setUp() {
    testContinent = Continent.builder()
        .id(1L)
        .name("Europe")
        .description("Europe description")
        .build();
    testContinent.generateSlug();
    testCountry = Country.builder()
        .id(1L)
        .name("France")
        .description("France description")
        .continent(testContinent)
        .build();
    testCountry.generateSlug();
    testContinent.setCountries(List.of(testCountry));
  }

  @Test
  void testGetContinentsWithCountriesShouldReturnDTOsWithCountriesOnly() {
    when(continentRepository.findAllWithCountriesByOrderByNameAsc()).thenReturn(List.of(testContinent));

    List<ContinentSummaryDTO> result = continentService.getContinentsWithCountries();

    assertNotNull(result);
    assertEquals(1, result.size());
    ContinentSummaryDTO dto = result.get(0);
    assertEquals(1L, dto.getId());
    assertEquals("Europe", dto.getName());
    assertEquals("europe", dto.getSlug());
    assertEquals("Europe description", dto.getDescription());
    assertNotNull(dto.getCountries());
    assertEquals(1, dto.getCountries().size());
    CountrySummaryDTO countryDto = dto.getCountries().get(0);
    assertEquals(1L, countryDto.getId());
    assertEquals("France", countryDto.getName());
    assertEquals("france", countryDto.getSlug());
    assertEquals("France description", countryDto.getDescription());
    verify(continentRepository).findAllWithCountriesByOrderByNameAsc();
  }

  @Test
  void testGetContinentsWithCountriesShouldReturnEmptyListWhenNoContinents() {
    when(continentRepository.findAllWithCountriesByOrderByNameAsc()).thenReturn(List.of());

    List<ContinentSummaryDTO> result = continentService.getContinentsWithCountries();

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(continentRepository).findAllWithCountriesByOrderByNameAsc();
  }

  @Test
  void testGetContinentsWithCountriesShouldHandleContinentWithNoCountries() {
    testContinent.setCountries(null);
    when(continentRepository.findAllWithCountriesByOrderByNameAsc()).thenReturn(List.of(testContinent));

    List<ContinentSummaryDTO> result = continentService.getContinentsWithCountries();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertNotNull(result.get(0).getCountries());
    assertTrue(result.get(0).getCountries().isEmpty());
    verify(continentRepository).findAllWithCountriesByOrderByNameAsc();
  }

  @Test
  void testGetContinentBySlugShouldReturnContinentWhenSlugExists() {
    when(continentRepository.findBySlug("europe")).thenReturn(Optional.of(testContinent));

    Continent result = continentService.getContinentBySlug("europe");

    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals("Europe", result.getName());
    assertEquals("europe", result.getSlug());
    verify(continentRepository).findBySlug("europe");
  }

  @Test
  void testGetContinentBySlugShouldThrowWhenSlugNotFound() {
    when(continentRepository.findBySlug("unknown")).thenReturn(Optional.empty());

    EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
        continentService.getContinentBySlug("unknown"));

    assertEquals("Continent not found", thrown.getMessage());
    verify(continentRepository).findBySlug("unknown");
  }
}
