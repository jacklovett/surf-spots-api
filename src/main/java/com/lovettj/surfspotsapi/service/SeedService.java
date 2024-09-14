package com.lovettj.surfspotsapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;

import jakarta.transaction.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeedService {

  private final ObjectMapper mapper = new ObjectMapper();

  private final ContinentRepository continentRepository;
  private final CountryRepository countryRepository;
  private final RegionRepository regionRepository;

  public SeedService(ContinentRepository continentRepository,
      CountryRepository countryRepository,
      RegionRepository regionRepository) {
    this.continentRepository = continentRepository;
    this.countryRepository = countryRepository;
    this.regionRepository = regionRepository;
  }

  @Transactional
  public void seedData() {
    try {
      seedContinents();
      seedCountries();
      seedRegions();
    } catch (Exception e) {
      System.out.println("Failed to seed data:");
      e.printStackTrace();
    }
  }

  private void seedContinents() throws IOException {
    File continentFile = new ClassPathResource("static/seedData/continents.json").getFile(); // Load file from classpath
    Continent[] continents;

    try {
      continents = mapper.readValue(continentFile, Continent[].class);
      if (continents == null || continents.length == 0) {
        throw new IllegalStateException("No continent seed data found");
      }
    } catch (IOException e) {
      System.out.println("Failed to retrieve Continents seed data: " + e.getMessage());
      throw e;
    }

    // Fetch existing continents in one go
    List<String> existingContinents = continentRepository.findAll()
        .stream()
        .map(Continent::getName)
        .collect(Collectors.toList());

    // Filter and save new continents
    List<Continent> newContinents = Arrays.stream(continents)
        .filter(continent -> !existingContinents.contains(continent.getName()))
        .collect(Collectors.toList());

    if (!newContinents.isEmpty()) {
      continentRepository.saveAll(newContinents); // Batch insert for performance
    }
  }

  private void seedCountries() throws IOException {
    File countryFile = new ClassPathResource("static/seedData/countries.json").getFile(); // Load file from classpath
    Country[] countries;

    try {
      countries = mapper.readValue(countryFile, Country[].class);
      if (countries == null || countries.length == 0) {
        throw new IllegalStateException("No country seed data found");
      }
    } catch (IOException e) {
      System.out.println("Failed to retrieve Countries seed data: " + e.getMessage());
      throw e;
    }

    // Fetch existing countries in one go
    List<String> existingCountries = countryRepository.findAll()
        .stream()
        .map(Country::getName)
        .collect(Collectors.toList());

    // Filter and save new countries
    List<Country> newCountries = Arrays.stream(countries)
        .filter(country -> !existingCountries.contains(country.getName()))
        .collect(Collectors.toList());

    if (!newCountries.isEmpty()) {
      countryRepository.saveAll(newCountries); // Batch insert for performance
    }
  }

  private void seedRegions() throws IOException {
    File regionFile = new ClassPathResource("static/seedData/regions.json").getFile(); // Load file from classpath
    Region[] regions;

    try {
      regions = mapper.readValue(regionFile, Region[].class);
      if (regions == null || regions.length == 0) {
        throw new IllegalStateException("No region seed data found");
      }
    } catch (IOException e) {
      System.out.println("Failed to retrieve regions seed data: " + e.getMessage());
      throw e;
    }

    // Fetch existing regions in one go
    List<String> existingRegions = regionRepository.findAll()
        .stream()
        .map(Region::getName)
        .collect(Collectors.toList());

    // Filter and save new regions
    List<Region> newRegions = Arrays.stream(regions)
        .filter(region -> !existingRegions.contains(region.getName()))
        .collect(Collectors.toList());

    if (!newRegions.isEmpty()) {
      regionRepository.saveAll(newRegions); // Batch insert for performance
    }
  }
}
