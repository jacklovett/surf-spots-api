package com.lovettj.surfspotsapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeedService {

  private static final Logger logger = LoggerFactory.getLogger(SeedService.class);
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
      logger.error("Error during seeding data: {}", e.getMessage(), e);
    }
  }

  private void seedContinents() {
    try {
      File continentFile = new ClassPathResource("static/seedData/continents.json").getFile(); // Load file from
                                                                                               // classpath
      Continent[] continents = mapper.readValue(continentFile, Continent[].class);

      if (continents == null || continents.length == 0) {
        throw new IllegalStateException("No continent seed data found");
      }

      List<String> existingContinents = continentRepository.findAll()
          .stream()
          .map(Continent::getName)
          .collect(Collectors.toList());

      List<Continent> newContinents = Arrays.stream(continents)
          .filter(continent -> !existingContinents.contains(continent.getName()))
          .collect(Collectors.toList());

      if (!newContinents.isEmpty()) {
        continentRepository.saveAll(newContinents); // Batch insert for performance
        logger.info("Successfully seeded {} continents", newContinents.size());
      } else {
        logger.info("No new continents to seed");
      }
    } catch (IOException e) {
      logger.error("Failed to read or parse Continent seed data: {}",
          e.getMessage(), e);
    } catch (DataAccessException e) {
      logger.error("Database access error while seeding continents: {}",
          e.getMessage(), e);
    }
  }

  private void seedCountries() {
    try {
      File countryFile = new ClassPathResource("static/seedData/countries.json").getFile(); // Load file from classpath
      Country[] countries = mapper.readValue(countryFile, Country[].class);

      if (countries == null || countries.length == 0) {
        throw new IllegalStateException("No country seed data found");
      }

      List<String> existingCountries = countryRepository.findAll()
          .stream()
          .map(Country::getName)
          .collect(Collectors.toList());

      List<Country> newCountries = Arrays.stream(countries)
          .filter(country -> !existingCountries.contains(country.getName()))
          .collect(Collectors.toList());

      if (!newCountries.isEmpty()) {
        countryRepository.saveAll(newCountries); // Batch insert for performance
        logger.info("Successfully seeded {} countries", newCountries.size());
      } else {
        logger.info("No new countries to seed");
      }
    } catch (IOException e) {
      logger.error("Failed to read or parse Country seed data: {}", e.getMessage(),
          e);
    } catch (DataAccessException e) {
      logger.error("Database access error while seeding countries: {}",
          e.getMessage(), e);
    }
  }

  private void seedRegions() {
    try {
      File regionFile = new ClassPathResource("static/seedData/regions.json").getFile(); // Load file from classpath
      Region[] regions = mapper.readValue(regionFile, Region[].class);

      if (regions == null || regions.length == 0) {
        throw new IllegalStateException("No region seed data found");
      }

      List<String> existingRegions = regionRepository.findAll()
          .stream()
          .map(Region::getName)
          .collect(Collectors.toList());

      List<Region> newRegions = Arrays.stream(regions)
          .filter(region -> !existingRegions.contains(region.getName()))
          .collect(Collectors.toList());

      if (!newRegions.isEmpty()) {
        regionRepository.saveAll(newRegions); // Batch insert for performance
        logger.info("Successfully seeded {} regions", newRegions.size());
      } else {
        logger.info("No new regions to seed");
      }
    } catch (IOException e) {
      logger.error("Failed to read or parse Region seed data: {}", e.getMessage(),
          e);
    } catch (DataAccessException e) {
      logger.error("Database access error while seeding regions: {}",
          e.getMessage(), e);
    }
  }
}
