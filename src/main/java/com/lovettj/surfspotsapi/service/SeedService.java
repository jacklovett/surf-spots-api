package com.lovettj.surfspotsapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;

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
  private final SurfSpotRepository surfSpotRepository;

  public SeedService(ContinentRepository continentRepository,
      CountryRepository countryRepository,
      RegionRepository regionRepository, SurfSpotRepository surfSpotRepository) {
    this.continentRepository = continentRepository;
    this.countryRepository = countryRepository;
    this.regionRepository = regionRepository;
    this.surfSpotRepository = surfSpotRepository;
  }

  @Transactional
  public void seedData() {
    try {
      seedContinents();
      seedCountries();
      seedRegions();
      seedSurfSpots();
    } catch (Exception e) {
      logger.error("Error during seeding data: {}", e.getMessage(), e);
    }
  }

  private void seedContinents() {
    seedEntities("continents.json", Continent[].class, continentRepository.findAll(), continentRepository::saveAll,
        Continent::getName);
  }

  private void seedCountries() {
    seedEntities("countries.json", Country[].class, countryRepository.findAll(), countryRepository::saveAll,
        Country::getName);
  }

  private void seedRegions() {
    seedEntities("regions.json", Region[].class, regionRepository.findAll(), regionRepository::saveAll,
        Region::getName);
  }

  private void seedSurfSpots() {
    seedEntities("surf-spots.json", SurfSpot[].class, surfSpotRepository.findAll(), surfSpotRepository::saveAll,
        SurfSpot::getName);
  }

  private <T> void seedEntities(String fileName, Class<T[]> entityType,
      List<T> existingEntities,
      java.util.function.Consumer<List<T>> saveAll,
      java.util.function.Function<T, String> getNameFunction) {
    try {
      File file = new ClassPathResource("static/seedData/" + fileName).getFile();
      T[] entities = mapper.readValue(file, entityType);

      if (entities == null || entities.length == 0) {
        throw new IllegalStateException("No seed data found in " + fileName);
      }

      List<String> existingEntityNames = existingEntities
          .stream()
          .map(getNameFunction)
          .collect(Collectors.toList());

      List<T> newEntities = Arrays.stream(entities)
          .filter(entity -> !existingEntityNames.contains(getNameFunction.apply(entity)))
          .collect(Collectors.toList());

      if (!newEntities.isEmpty()) {
        saveAll.accept(newEntities);
        logger.info("Successfully seeded {} entries from {}", newEntities.size(), fileName);
      } else {
        logger.info("No new entries to seed from {}", fileName);
      }
    } catch (IOException e) {
      logger.error("Failed to read or parse seed data from {}: {}", fileName, e.getMessage(), e);
    } catch (DataAccessException e) {
      logger.error("Database access error while seeding from {}: {}", fileName, e.getMessage(), e);
    }
  }
}
