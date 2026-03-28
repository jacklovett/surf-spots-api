package com.lovettj.surfspotsapi.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SwellSeason;
import com.lovettj.surfspotsapi.repository.ContinentRepository;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SubRegionRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.SwellSeasonRepository;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class SeedService {

  private static final Logger logger = LoggerFactory.getLogger(SeedService.class);
  private final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final ContinentRepository continentRepository;
  private final CountryRepository countryRepository;
  private final RegionRepository regionRepository;
  private final SubRegionRepository subRegionRepository;
  private final SurfSpotRepository surfSpotRepository;
  private final SwellSeasonRepository swellSeasonRepository;
  private final SwellSeasonDeterminationService swellSeasonDeterminationService;

  @Value("${app.seed.enabled:true}")
  private boolean seedEnabled;

  public SeedService(
      ContinentRepository continentRepository,
      CountryRepository countryRepository,
      RegionRepository regionRepository,
      SubRegionRepository subRegionRepository,
      SurfSpotRepository surfSpotRepository,
      SwellSeasonRepository swellSeasonRepository,
      SwellSeasonDeterminationService swellSeasonDeterminationService) {
    this.continentRepository = continentRepository;
    this.countryRepository = countryRepository;
    this.regionRepository = regionRepository;
    this.subRegionRepository = subRegionRepository;
    this.surfSpotRepository = surfSpotRepository;
    this.swellSeasonRepository = swellSeasonRepository;
    this.swellSeasonDeterminationService = swellSeasonDeterminationService;
  }

  /**
   * Loads reference data from JSON once into an empty database. If continents already exist, does nothing
   * (re-seeding / syncing from JSON is not supported here; use migrations for production data changes).
   */
  @Transactional
  public void seedData() {
    if (!seedEnabled) {
      logger.info("Automatic seeding is disabled. Skipping seed data initialization.");
      return;
    }
    if (continentRepository.count() > 0) {
      logger.info("Database already seeded (continents present); skipping.");
      return;
    }
    insertSwellSeasons();
    insertContinents();
    insertCountries();
    insertRegions();
    insertSubRegions();
    insertSurfSpots();
  }

  private void insertSwellSeasons() {
    insertFromJson("swell-seasons.json", SwellSeason[].class, swellSeasonRepository, "swell seasons");
  }

  private void insertContinents() {
    insertFromJson("continents.json", Continent[].class, continentRepository, "continents");
  }

  private void insertCountries() {
    try {
      Resource resource = getMainResource("static/seedData/countries.json");
      Country[] entities = mapper.readValue(resource.getInputStream(), Country[].class);
      if (entities == null || entities.length == 0) {
        throw new IllegalStateException("No seed data found in countries.json");
      }
      logger.info("Read {} total entities from countries.json", entities.length);
      List<Country> list = new ArrayList<>(Arrays.asList(entities));
      List<Continent> allContinents = continentRepository.findAllByOrderByNameAsc();
      for (Country country : list) {
        if (country.getContinent() != null) {
          Continent continent = null;
          if (country.getContinent().getName() != null && !country.getContinent().getName().isBlank()) {
            continent =
                continentRepository.findByNameIgnoreCase(country.getContinent().getName()).orElse(null);
          }
          if (continent == null && country.getContinent().getId() != null) {
            int index = country.getContinent().getId().intValue() - 1;
            if (index >= 0 && index < allContinents.size()) {
              continent = allContinents.get(index);
            }
          }
          if (continent == null) {
            logger.warn(
                "Continent not found for country '{}' (name={}, id={}), skipping continent reference",
                country.getName(),
                country.getContinent().getName(),
                country.getContinent().getId());
          }
          country.setContinent(continent);
        }
        if (country.getEmergencyNumbers() != null) {
          country.getEmergencyNumbers().forEach(en -> en.setCountry(country));
        }
      }
      countryRepository.saveAll(list);
      countryRepository.flush();
      logger.info("Inserted {} countries from countries.json", list.size());
    } catch (IOException e) {
      logger.error("Failed to read or parse seed data from countries.json: {}", e.getMessage(), e);
      throw new IllegalStateException(e);
    } catch (DataAccessException e) {
      logger.error("Database access error while seeding from countries.json: {}", e.getMessage(), e);
      throw e;
    }
  }

  private void insertRegions() {
    try {
      Resource resource = getMainResource("static/seedData/regions.json");
      Region[] entities = mapper.readValue(resource.getInputStream(), Region[].class);
      if (entities == null || entities.length == 0) {
        throw new IllegalStateException("No seed data found in regions.json");
      }
      logger.info("Read {} total entities from regions.json", entities.length);
      List<Country> allCountries = countryRepository.findAllByOrderByContinentNameAscNameAsc();
      List<Region> list = new ArrayList<>();
      for (Region jsonEntity : entities) {
        Region row = new Region();
        row.setName(jsonEntity.getName());
        row.setDescription(jsonEntity.getDescription());
        row.setBoundingBox(jsonEntity.getBoundingBox());
        if (jsonEntity.getCountry() != null) {
          Country country = null;
          if (jsonEntity.getCountry().getName() != null && !jsonEntity.getCountry().getName().isBlank()) {
            country = countryRepository.findByNameIgnoreCase(jsonEntity.getCountry().getName()).orElse(null);
            if (country == null) {
              logger.warn(
                  "Country name '{}' not found for region '{}', trying index fallback",
                  jsonEntity.getCountry().getName(),
                  jsonEntity.getName());
            }
          }
          if (country == null && jsonEntity.getCountry().getId() != null) {
            int index = jsonEntity.getCountry().getId().intValue() - 1;
            if (index >= 0 && index < allCountries.size()) {
              country = allCountries.get(index);
            }
          }
          if (country == null) {
            logger.warn(
                "Country not found for region '{}' (name={}, id={}), skipping country reference",
                jsonEntity.getName(),
                jsonEntity.getCountry().getName(),
                jsonEntity.getCountry().getId());
          }
          row.setCountry(country);
        }
        list.add(row);
      }
      List<Region> deduplicated =
          new ArrayList<>(
              list.stream()
                  .collect(
                      Collectors.toMap(
                          r -> regionKey(r.getCountry() != null ? r.getCountry().getId() : null, r.getName()),
                          r -> r,
                          (first, second) -> first))
                  .values());
      regionRepository.saveAll(deduplicated);
      regionRepository.flush();
      logger.info("Inserted {} regions from regions.json", deduplicated.size());
    } catch (IOException e) {
      logger.error("Failed to read or parse seed data from regions.json: {}", e.getMessage(), e);
      throw new IllegalStateException(e);
    } catch (DataAccessException e) {
      logger.error("Database access error while seeding from regions.json: {}", e.getMessage(), e);
      throw e;
    }
  }

  private void insertSubRegions() {
    try {
      Resource resource = getMainResource("static/seedData/sub-regions.json");
      SubRegion[] entities = mapper.readValue(resource.getInputStream(), SubRegion[].class);
      if (entities == null || entities.length == 0) {
        logger.info("No seed data found in sub-regions.json - skipping sub-region seeding");
        return;
      }
      logger.info("Read {} total entities from sub-regions.json", entities.length);
      List<Region> allRegions = regionRepository.findAllByOrderByCountryNameAscNameAsc();
      List<SubRegion> list = new ArrayList<>();
      for (SubRegion jsonEntity : entities) {
        SubRegion row = new SubRegion();
        row.setName(jsonEntity.getName());
        row.setDescription(jsonEntity.getDescription());
        if (jsonEntity.getRegion() != null && jsonEntity.getRegion().getId() != null) {
          int index = jsonEntity.getRegion().getId().intValue() - 1;
          if (index >= 0 && index < allRegions.size()) {
            row.setRegion(allRegions.get(index));
          } else {
            logger.warn(
                "Region index {} not found for sub-region '{}'",
                jsonEntity.getRegion().getId(),
                jsonEntity.getName());
          }
        }
        list.add(row);
      }
      subRegionRepository.saveAll(list);
      subRegionRepository.flush();
      logger.info("Inserted {} sub-regions from sub-regions.json", list.size());
    } catch (IOException e) {
      logger.error("Failed to read or parse seed data from sub-regions.json: {}", e.getMessage(), e);
      throw new IllegalStateException(e);
    } catch (DataAccessException e) {
      logger.error("Database access error while seeding from sub-regions.json: {}", e.getMessage(), e);
      throw e;
    }
  }

  private void insertSurfSpots() {
    try {
      Resource resource = getMainResource("static/seedData/surf-spots.json");
      SurfSpot[] entities = mapper.readValue(resource.getInputStream(), SurfSpot[].class);
      if (entities == null || entities.length == 0) {
        logger.info("No seed data found in surf-spots.json - skipping surf spot seeding");
        return;
      }
      logger.info("Read {} total entities from surf-spots.json", entities.length);
      List<Region> allRegions = regionRepository.findAllByOrderByCountryNameAscNameAsc();
      List<SubRegion> allSubRegions = subRegionRepository.findAllByOrderByRegionNameAscNameAsc();
      List<SurfSpot> list = new ArrayList<>();
      for (SurfSpot spot : entities) {
        if (spot.getRegion() != null && spot.getRegion().getId() != null) {
          Long jsonRegionId = spot.getRegion().getId();
          int index = jsonRegionId.intValue() - 1;
          if (index >= 0 && index < allRegions.size()) {
            spot.setRegion(allRegions.get(index));
          } else {
            logger.warn(
                "Region with JSON id {} (index {}) not found for surf spot '{}', skipping region reference. Total regions: {}",
                jsonRegionId,
                index,
                spot.getName(),
                allRegions.size());
            spot.setRegion(null);
          }
        }
        if (spot.getSubRegion() != null && spot.getSubRegion().getId() != null) {
          Long jsonSubRegionId = spot.getSubRegion().getId();
          int index = jsonSubRegionId.intValue() - 1;
          if (index >= 0 && index < allSubRegions.size()) {
            spot.setSubRegion(allSubRegions.get(index));
          } else {
            logger.warn(
                "SubRegion with JSON id {} (index {}) not found for surf spot '{}', skipping sub-region reference. Total sub-regions: {}",
                jsonSubRegionId,
                index,
                spot.getName(),
                allSubRegions.size());
            spot.setSubRegion(null);
          }
        }
        boolean skipSwell =
            Boolean.TRUE.equals(spot.getIsWavepool()) || Boolean.TRUE.equals(spot.getIsRiverWave());
        if (!skipSwell && spot.getLatitude() != null && spot.getLongitude() != null) {
          swellSeasonDeterminationService
              .determineSwellSeason(spot.getLatitude(), spot.getLongitude())
              .ifPresent(spot::setSwellSeason);
        }
        list.add(spot);
      }
      surfSpotRepository.saveAll(list);
      surfSpotRepository.flush();
      logger.info("Inserted {} surf spots from surf-spots.json", list.size());
    } catch (IOException e) {
      logger.error("Failed to read or parse seed data from surf-spots.json: {}", e.getMessage(), e);
      throw new IllegalStateException(e);
    } catch (DataAccessException e) {
      logger.error("Database access error while seeding from surf-spots.json: {}", e.getMessage(), e);
      throw e;
    }
  }

  private <T> void insertFromJson(
      String fileName, Class<T[]> entityType, JpaRepository<T, ?> repository, String label) {
    try {
      Resource resource = getMainResource("static/seedData/" + fileName);
      T[] entities = mapper.readValue(resource.getInputStream(), entityType);
      if (entities == null || entities.length == 0) {
        throw new IllegalStateException("No seed data found in " + fileName);
      }
      logger.info("Read {} total entities from {}", entities.length, fileName);
      List<T> list = Arrays.asList(entities);
      repository.saveAll(list);
      repository.flush();
      logger.info("Inserted {} {} from {}", list.size(), label, fileName);
    } catch (IOException e) {
      logger.error("Failed to read or parse seed data from {}: {}", fileName, e.getMessage(), e);
      throw new IllegalStateException(e);
    } catch (DataAccessException e) {
      logger.error("Database access error while seeding from {}: {}", fileName, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Loads a resource from the classpath, preferring main resources over test resources. When both exist
   * (e.g. in tests), uses the one from main so production/dev behaviour is unchanged.
   */
  private Resource getMainResource(String path) throws IOException {
    ClassLoader loader = SeedService.class.getClassLoader();
    if (loader == null) {
      loader = ClassLoader.getSystemClassLoader();
    }
    List<URL> urls = Collections.list(loader.getResources(path));
    URL mainUrl =
        urls.stream()
            .filter(url -> !url.toString().contains("test-classes"))
            .findFirst()
            .orElseGet(() -> urls.isEmpty() ? null : urls.get(0));
    if (mainUrl == null) {
      throw new IOException("Resource not found: " + path);
    }
    logger.debug("Loading resource from: {}", mainUrl);
    return new UrlResource(mainUrl);
  }

  private static String regionKey(Long countryId, String name) {
    return (countryId != null ? countryId : 0L) + "|" + (name != null ? name : "");
  }
}
