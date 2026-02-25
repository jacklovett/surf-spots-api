package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.ContinentSummaryDTO;
import com.lovettj.surfspotsapi.dto.CountrySummaryDTO;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.repository.ContinentRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContinentService {
  private final ContinentRepository continentRepository;

  public ContinentService(ContinentRepository continentRepository) {
    this.continentRepository = continentRepository;
  }

  /**
   * Returns continents with their countries.
   */
  public List<ContinentSummaryDTO> getContinentsWithCountries() {
    return continentRepository.findAllWithCountriesByOrderByNameAsc().stream()
        .map(this::toContinentSummaryDTO)
        .collect(Collectors.toList());
  }
  private ContinentSummaryDTO toContinentSummaryDTO(Continent c) {
    List<CountrySummaryDTO> countries = c.getCountries() == null
        ? List.of()
        : c.getCountries().stream()
            .map(this::toCountrySummaryDTO)
            .collect(Collectors.toList());
    return ContinentSummaryDTO.builder()
        .id(c.getId())
        .name(c.getName())
        .slug(c.getSlug())
        .description(c.getDescription())
        .countries(countries)
        .build();
  }

  private CountrySummaryDTO toCountrySummaryDTO(Country country) {
    return CountrySummaryDTO.builder()
        .id(country.getId())
        .name(country.getName())
        .slug(country.getSlug())
        .description(country.getDescription())
        .build();
  }

  public Continent getContinentBySlug(String slug) {
    return continentRepository.findBySlug(slug).orElseThrow(() -> new EntityNotFoundException("Continent not found"));
  }
}
