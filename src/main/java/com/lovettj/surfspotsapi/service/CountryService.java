package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CountryService {
  private final CountryRepository countryRepository;
  private final ContinentRepository continentRepository;

  public CountryService(CountryRepository countryRepository, ContinentRepository continentRepository) {
    this.countryRepository = countryRepository;
    this.continentRepository = continentRepository;
  }

  public Country getCountryBySlug(String slug) {
    return countryRepository.findBySlug(slug).orElseThrow(() -> new EntityNotFoundException("Country not found"));
  }

  public List<Country> getCountriesByContinent(String continentSlug) {
    Continent continent = continentRepository.findBySlug(continentSlug)
        .orElseThrow(() -> new EntityNotFoundException("Continent not found"));
    return countryRepository.findByContinent(continent);
  }

  public List<Country> getAllCountries() {
    return countryRepository.findAll();
  }

  /**
   * Find country by name (case-insensitive).
   * Used for matching Mapbox reverse geocoding results.
   */
  public Optional<Country> findCountryByName(String name) {
    return countryRepository.findByNameIgnoreCase(name);
  }
}
