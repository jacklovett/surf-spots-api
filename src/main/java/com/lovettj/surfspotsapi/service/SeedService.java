package com.lovettj.surfspotsapi.service;

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
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

@Service
public class SeedService {

    private static final Logger logger = LoggerFactory.getLogger(SeedService.class);
    private final ObjectMapper mapper = new ObjectMapper();

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
            SwellSeasonDeterminationService swellSeasonDeterminationService
    ) {
        this.continentRepository = continentRepository;
        this.countryRepository = countryRepository;
        this.regionRepository = regionRepository;
        this.subRegionRepository = subRegionRepository;
        this.surfSpotRepository = surfSpotRepository;
        this.swellSeasonRepository = swellSeasonRepository;
        this.swellSeasonDeterminationService = swellSeasonDeterminationService;
    }

    @PostConstruct
    public void init() {
        if (seedEnabled) {
            logger.info("Automatic seeding is enabled. Starting seed data initialization...");
            seedData();
        } else {
            logger.info("Automatic seeding is disabled. Skipping seed data initialization.");
        }
    }

    public void seedData() {
        try {
            seedSwellSeasons();
            seedContinents();
            seedCountries();
            seedRegions();
            seedSubRegions();
            seedSurfSpots();
        } catch (Exception e) {
            logger.error("Error during seeding data: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void seedSwellSeasons() {
        seedEntities("swell-seasons.json", SwellSeason[].class, swellSeasonRepository,
                SwellSeason::getName, (existing, jsonEntity) -> {
                    existing.setStartMonth(jsonEntity.getStartMonth());
                    existing.setEndMonth(jsonEntity.getEndMonth());
                });
    }

    @Transactional
    public void seedContinents() {
        seedEntities("continents.json", Continent[].class, continentRepository,
                Continent::getName, (existing, jsonEntity) -> {
                    existing.setDescription(jsonEntity.getDescription());
                });
    }

    @Transactional
    public void seedCountries() {
        try {
            Resource resource = getMainResource("static/seedData/countries.json");
            Country[] entities = mapper.readValue(resource.getInputStream(), Country[].class);

            if (entities == null || entities.length == 0) {
                throw new IllegalStateException("No seed data found in countries.json");
            }

            logger.info("Read {} total entities from countries.json", entities.length);

            // Create map of existing entities by name
            // Handle duplicates by keeping the first occurrence
            Map<String, Country> existingMap = countryRepository.findAll().stream()
                    .collect(Collectors.toMap(Country::getName, c -> c, (first, second) -> {
                        logger.warn("Duplicate country name found in database: {}. Keeping first occurrence.", first.getName());
                        return first;
                    }));

            List<Country> toSave = Arrays.stream(entities)
                    .map(jsonEntity -> {
                        Country existing = existingMap.get(jsonEntity.getName());
                        if (existing != null) {
                            existing.setDescription(jsonEntity.getDescription());
                            existing.setContinent(jsonEntity.getContinent());
                            logger.debug("Updating existing country: {}", existing.getName());
                            return existing;
                        } else {
                            logger.debug("Creating new country: {}", jsonEntity.getName());
                            return jsonEntity;
                        }
                    })
                    .collect(Collectors.toList());

            // Resolve continent references by position (JSON ID - 1 = 0-based index)
            List<Continent> allContinents = continentRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());
            for (Country country : toSave) {
                if (country.getContinent() != null && country.getContinent().getId() != null) {
                    Long jsonContinentId = country.getContinent().getId();
                    int index = jsonContinentId.intValue() - 1;
                    if (index >= 0 && index < allContinents.size()) {
                        country.setContinent(allContinents.get(index));
                    } else {
                        logger.warn("Continent with JSON id {} (index {}) not found for country '{}', skipping continent reference. Total continents: {}",
                                jsonContinentId, index, country.getName(), allContinents.size());
                        country.setContinent(null);
                    }
                }
            }

            saveAndLog(countryRepository, toSave, existingMap, Country::getName, "countries", "countries.json");
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from countries.json: {}", e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from countries.json: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void seedRegions() {
        try {
            Resource resource = getMainResource("static/seedData/regions.json");
            Region[] entities = mapper.readValue(resource.getInputStream(), Region[].class);

            if (entities == null || entities.length == 0) {
                throw new IllegalStateException("No seed data found in regions.json");
            }

            logger.info("Read {} total entities from regions.json", entities.length);

            // Create map of existing entities by name
            // Handle duplicates by keeping the first occurrence
            Map<String, Region> existingMap = regionRepository.findAll().stream()
                    .collect(Collectors.toMap(Region::getName, r -> r, (first, second) -> {
                        logger.warn("Duplicate region name found in database: {}. Keeping first occurrence.", first.getName());
                        return first;
                    }));

            List<Region> toSave = Arrays.stream(entities)
                    .map(jsonEntity -> {
                        Region existing = existingMap.get(jsonEntity.getName());
                        if (existing != null) {
                            existing.setDescription(jsonEntity.getDescription());
                            existing.setBoundingBox(jsonEntity.getBoundingBox());
                            existing.setCountry(jsonEntity.getCountry());
                            logger.debug("Updating existing region: {}", existing.getName());
                            return existing;
                        } else {
                            logger.debug("Creating new region: {}", jsonEntity.getName());
                            return jsonEntity;
                        }
                    })
                    .collect(Collectors.toList());

            // Resolve country references by position (JSON ID - 1 = 0-based index)
            List<Country> allCountries = countryRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());
            for (Region region : toSave) {
                if (region.getCountry() != null && region.getCountry().getId() != null) {
                    Long jsonCountryId = region.getCountry().getId();
                    int index = jsonCountryId.intValue() - 1;
                    if (index >= 0 && index < allCountries.size()) {
                        region.setCountry(allCountries.get(index));
                    } else {
                        logger.warn("Country with JSON id {} (index {}) not found for region '{}', skipping country reference. Total countries: {}",
                                jsonCountryId, index, region.getName(), allCountries.size());
                        region.setCountry(null);
                    }
                }
            }

            saveAndLog(regionRepository, toSave, existingMap, Region::getName, "regions", "regions.json");
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from regions.json: {}", e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from regions.json: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void seedSubRegions() {
        try {
            Resource resource = getMainResource("static/seedData/sub-regions.json");
            SubRegion[] entities = mapper.readValue(resource.getInputStream(), SubRegion[].class);

            if (entities == null || entities.length == 0) {
                logger.info("No seed data found in sub-regions.json - skipping sub-region seeding");
                return;
            }

            logger.info("Read {} total entities from sub-regions.json", entities.length);

            // Create map of existing entities by name
            // Handle duplicates by keeping the first occurrence
            Map<String, SubRegion> existingMap = subRegionRepository.findAll().stream()
                    .collect(Collectors.toMap(SubRegion::getName, sr -> sr, (first, second) -> {
                        logger.warn("Duplicate sub-region name found in database: {}. Keeping first occurrence.", first.getName());
                        return first;
                    }));

            List<SubRegion> toSave = Arrays.stream(entities)
                    .map(jsonEntity -> {
                        SubRegion existing = existingMap.get(jsonEntity.getName());
                        if (existing != null) {
                            existing.setDescription(jsonEntity.getDescription());
                            existing.setRegion(jsonEntity.getRegion());
                            logger.debug("Updating existing sub-region: {}", existing.getName());
                            return existing;
                        } else {
                            logger.debug("Creating new sub-region: {}", jsonEntity.getName());
                            return jsonEntity;
                        }
                    })
                    .collect(Collectors.toList());

            // Resolve region references by position (JSON ID - 1 = 0-based index)
            List<Region> allRegions = regionRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());
            for (SubRegion subRegion : toSave) {
                if (subRegion.getRegion() != null && subRegion.getRegion().getId() != null) {
                    Long jsonRegionId = subRegion.getRegion().getId();
                    int index = jsonRegionId.intValue() - 1;
                    if (index >= 0 && index < allRegions.size()) {
                        subRegion.setRegion(allRegions.get(index));
                    } else {
                        logger.warn("Region with JSON id {} (index {}) not found for sub-region '{}', skipping region reference. Total regions: {}",
                                jsonRegionId, index, subRegion.getName(), allRegions.size());
                        subRegion.setRegion(null);
                    }
                }
            }

            saveAndLog(subRegionRepository, toSave, existingMap, SubRegion::getName, "sub-regions", "sub-regions.json");
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from sub-regions.json: {}", e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from sub-regions.json: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void seedSurfSpots() {
        try {
            Resource resource = getMainResource("static/seedData/surf-spots.json");
            SurfSpot[] entities = mapper.readValue(resource.getInputStream(), SurfSpot[].class);

            if (entities == null || entities.length == 0) {
                logger.info("No seed data found in surf-spots.json - skipping surf spot seeding");
                return;
            }

            logger.info("Read {} total entities from surf-spots.json", entities.length);

            // Create map of existing entities by name
            // Handle duplicates by keeping the first occurrence
            Map<String, SurfSpot> existingMap = surfSpotRepository.findAll().stream()
                    .collect(Collectors.toMap(SurfSpot::getName, ss -> ss, (first, second) -> {
                        logger.warn("Duplicate surf spot name found in database: {}. Keeping first occurrence.", first.getName());
                        return first;
                    }));

            List<SurfSpot> toSave = Arrays.stream(entities)
                    .map(jsonEntity -> {
                        SurfSpot existing = existingMap.get(jsonEntity.getName());
                        if (existing != null) {
                            // Update existing entity - preserve ID and timestamps
                            existing.setDescription(jsonEntity.getDescription());
                            existing.setLatitude(jsonEntity.getLatitude());
                            existing.setLongitude(jsonEntity.getLongitude());
                            existing.setType(jsonEntity.getType());
                            existing.setBeachBottomType(jsonEntity.getBeachBottomType());
                            existing.setSwellDirection(jsonEntity.getSwellDirection());
                            existing.setWindDirection(jsonEntity.getWindDirection());
                            existing.setSkillLevel(jsonEntity.getSkillLevel());
                            existing.setTide(jsonEntity.getTide());
                            existing.setWaveDirection(jsonEntity.getWaveDirection());
                            existing.setMinSurfHeight(jsonEntity.getMinSurfHeight());
                            existing.setMaxSurfHeight(jsonEntity.getMaxSurfHeight());
                            existing.setRating(jsonEntity.getRating());
                            existing.setFoodNearby(jsonEntity.getFoodNearby());
                            existing.setFoodOptions(jsonEntity.getFoodOptions());
                            existing.setAccommodationNearby(jsonEntity.getAccommodationNearby());
                            existing.setAccommodationOptions(jsonEntity.getAccommodationOptions());
                            existing.setFacilities(jsonEntity.getFacilities());
                            existing.setHazards(jsonEntity.getHazards());
                            existing.setParking(jsonEntity.getParking());
                            existing.setBoatRequired(jsonEntity.getBoatRequired());
                            existing.setIsWavepool(jsonEntity.getIsWavepool());
                            existing.setWavepoolUrl(jsonEntity.getWavepoolUrl());
                            existing.setForecasts(jsonEntity.getForecasts());
                            existing.setCreatedBy(jsonEntity.getCreatedBy());
                            existing.setStatus(jsonEntity.getStatus());
                            existing.setRegion(jsonEntity.getRegion());
                            existing.setSubRegion(jsonEntity.getSubRegion());
                            logger.debug("Updating existing surf spot: {}", existing.getName());
                            return existing;
                        } else {
                            logger.debug("Creating new surf spot: {}", jsonEntity.getName());
                            return jsonEntity;
                        }
                    })
                    .collect(Collectors.toList());

            // Resolve region and sub-region references by position (JSON ID - 1 = 0-based index)
            List<Region> allRegions = regionRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());
            List<SubRegion> allSubRegions = subRegionRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());
            for (SurfSpot surfSpot : toSave) {
                if (surfSpot.getRegion() != null && surfSpot.getRegion().getId() != null) {
                    Long jsonRegionId = surfSpot.getRegion().getId();
                    int index = jsonRegionId.intValue() - 1;
                    if (index >= 0 && index < allRegions.size()) {
                        surfSpot.setRegion(allRegions.get(index));
                    } else {
                        logger.warn("Region with JSON id {} (index {}) not found for surf spot '{}', skipping region reference. Total regions: {}",
                                jsonRegionId, index, surfSpot.getName(), allRegions.size());
                        surfSpot.setRegion(null);
                    }
                }
                if (surfSpot.getSubRegion() != null && surfSpot.getSubRegion().getId() != null) {
                    Long jsonSubRegionId = surfSpot.getSubRegion().getId();
                    int index = jsonSubRegionId.intValue() - 1;
                    if (index >= 0 && index < allSubRegions.size()) {
                        surfSpot.setSubRegion(allSubRegions.get(index));
                    } else {
                        logger.warn("SubRegion with JSON id {} (index {}) not found for surf spot '{}', skipping sub-region reference. Total sub-regions: {}",
                                jsonSubRegionId, index, surfSpot.getName(), allSubRegions.size());
                        surfSpot.setSubRegion(null);
                    }
                }
                // Automatically determine swell season based on coordinates (skip for wavepools)
                if (surfSpot.getIsWavepool() == null || !surfSpot.getIsWavepool()) {
                    if (surfSpot.getLatitude() != null && surfSpot.getLongitude() != null) {
                        swellSeasonDeterminationService.determineSwellSeason(
                                surfSpot.getLatitude(), 
                                surfSpot.getLongitude()
                        ).ifPresent(surfSpot::setSwellSeason);
                    }
                }
            }

            saveAndLog(surfSpotRepository, toSave, existingMap, SurfSpot::getName, "surf spots", "surf-spots.json");
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from surf-spots.json: {}", e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from surf-spots.json: {}", e.getMessage(), e);
        }
    }

    /**
     * Loads a resource from the classpath, preferring main resources over test resources.
     * When both exist (e.g. in tests), uses the one from main so production/dev behaviour is unchanged.
     */
    private Resource getMainResource(String path) throws IOException {
        ClassLoader loader = SeedService.class.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        List<URL> urls = Collections.list(loader.getResources(path));
        URL mainUrl = urls.stream()
                .filter(url -> !url.toString().contains("test-classes"))
                .findFirst()
                .orElseGet(() -> urls.isEmpty() ? null : urls.get(0));
        if (mainUrl == null) {
            throw new IOException("Resource not found: " + path);
        }
        logger.debug("Loading resource from: {}", mainUrl);
        return new UrlResource(mainUrl);
    }

    private <T> void seedEntities(
            String fileName,
            Class<T[]> entityType,
            JpaRepository<T, ?> repository,
            Function<T, String> getNameFunction,
            BiConsumer<T, T> updateFunction) {
        try {
            Resource resource = getMainResource("static/seedData/" + fileName);
            T[] entities = mapper.readValue(resource.getInputStream(), entityType);

            if (entities == null || entities.length == 0) {
                throw new IllegalStateException("No seed data found in " + fileName);
            }

            logger.info("Read {} total entities from {}", entities.length, fileName);

            // Handle duplicates by keeping the first occurrence
            Map<String, T> existingMap = repository.findAll().stream()
                    .collect(Collectors.toMap(getNameFunction, e -> e, (first, second) -> {
                        logger.warn("Duplicate entity name found in database: {}. Keeping first occurrence.", getNameFunction.apply(first));
                        return first;
                    }));

            List<T> toSave = Arrays.stream(entities)
                    .map(jsonEntity -> {
                        T existing = existingMap.get(getNameFunction.apply(jsonEntity));
                        if (existing != null) {
                            updateFunction.accept(existing, jsonEntity);
                            logger.debug("Updating existing entity: {}", getNameFunction.apply(existing));
                            return existing;
                        } else {
                            logger.debug("Creating new entity: {}", getNameFunction.apply(jsonEntity));
                            return jsonEntity;
                        }
                    })
                    .collect(Collectors.toList());

            saveAndLog(repository, toSave, existingMap, getNameFunction, getEntityNameFromFileName(fileName), fileName);
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from {}: {}", fileName, e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from {}: {}", fileName, e.getMessage(), e);
        }
    }

    private String getEntityNameFromFileName(String fileName) {
        return fileName.replace(".json", "").replace("-", " ");
    }

    private <T> void saveAndLog(
            JpaRepository<T, ?> repository,
            List<T> toSave,
            Map<String, T> existingMap,
            Function<T, String> getNameFunction,
            String entityName,
            String fileName) {
        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
            repository.flush();
            long updated = toSave.stream().filter(e -> existingMap.containsKey(getNameFunction.apply(e))).count();
            long created = toSave.size() - updated;
            logger.info("Successfully processed {} {} from {} ({} created, {} updated)",
                    toSave.size(), entityName, fileName, created, updated);
        } else {
            logger.info("No entries to process from {}", fileName);
        }
    }
}