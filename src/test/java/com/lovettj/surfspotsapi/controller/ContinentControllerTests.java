package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.ContinentSummaryDTO;
import com.lovettj.surfspotsapi.dto.CountrySummaryDTO;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.service.ContinentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContinentControllerTests {

  @Mock
  private ContinentService continentService;

  @InjectMocks
  private ContinentController continentController;

  private List<ContinentSummaryDTO> testContinents;

  @BeforeEach
  void setUp() {
    CountrySummaryDTO country = CountrySummaryDTO.builder()
        .id(1L)
        .name("France")
        .slug("france")
        .description(null)
        .build();
    ContinentSummaryDTO continent = ContinentSummaryDTO.builder()
        .id(1L)
        .name("Europe")
        .slug("europe")
        .description(null)
        .countries(List.of(country))
        .build();
    testContinents = List.of(continent);
  }

  @Test
  void testGetContinentsShouldReturnOkWithContinentSummaryDTOs() {
    when(continentService.getContinentsWithCountries()).thenReturn(testContinents);

    ResponseEntity<List<ContinentSummaryDTO>> response = continentController.getContinents();

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    ContinentSummaryDTO dto = response.getBody().get(0);
    assertEquals(1L, dto.getId());
    assertEquals("Europe", dto.getName());
    assertEquals("europe", dto.getSlug());
    assertNotNull(dto.getCountries());
    assertEquals(1, dto.getCountries().size());
    assertEquals("France", dto.getCountries().get(0).getName());
    assertEquals("france", dto.getCountries().get(0).getSlug());
    verify(continentService).getContinentsWithCountries();
  }

  @Test
  void testGetContinentsShouldReturnOkWithEmptyListWhenNoContinents() {
    when(continentService.getContinentsWithCountries()).thenReturn(Collections.emptyList());

    ResponseEntity<List<ContinentSummaryDTO>> response = continentController.getContinents();

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());
    verify(continentService).getContinentsWithCountries();
  }

  @Test
  void testGetContinentBySlugShouldReturnOkWithContinent() {
    Continent entity = Continent.builder().id(1L).name("Europe").build();
    entity.generateSlug();
    when(continentService.getContinentBySlug("europe")).thenReturn(entity);

    ResponseEntity<Continent> response = continentController.getContinentBySlug("europe");

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(1L, response.getBody().getId());
    assertEquals("Europe", response.getBody().getName());
    assertEquals("europe", response.getBody().getSlug());
    verify(continentService).getContinentBySlug("europe");
  }

  @Test
  void testGetContinentBySlugShouldThrowWhenNotFound() {
    when(continentService.getContinentBySlug("unknown"))
        .thenThrow(new EntityNotFoundException("Continent not found"));

    assertThrows(EntityNotFoundException.class, () ->
        continentController.getContinentBySlug("unknown"));
    verify(continentService).getContinentBySlug("unknown");
  }
}
