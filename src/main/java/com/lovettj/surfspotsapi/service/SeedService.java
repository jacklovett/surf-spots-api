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
                            if (jsonEntity.getEmergencyNumbers() != null && !jsonEntity.getEmergencyNumbers().isEmpty()) {
                                if (existing.getEmergencyNumbers() == null) {
                                    existing.setEmergencyNumbers(new java.util.ArrayList<>());
                                }
                                existing.getEmergencyNumbers().clear();
                                existing.getEmergencyNumbers().addAll(jsonEntity.getEmergencyNumbers());
                                existing.getEmergencyNumbers().forEach(en -> en.setCountry(existing));
                            }
                            logger.debug("Updating existing country: {}", existing.getName());
                            return existing;
                        } else {
                            logger.debug("Creating new country: {}", jsonEntity.getName());
                            return jsonEntity;
                        }
                    })
                    .collect(Collectors.toList());

            // Resolve continent by name from JSON first so we don't rely on index (avoids wrong continent when DB order differs from export)
            List<Continent> allContinents = continentRepository.findAllByOrderByNameAsc();
            for (Country country : toSave) {
                if (country.getContinent() != null) {
                    Continent continent = null;
                    if (country.getContinent().getName() != null && !country.getContinent().getName().isBlank()) {
                        continent = continentRepository.findByNameIgnoreCase(country.getContinent().getName()).orElse(null);
                    }
                    if (continent == null && country.getContinent().getId() != null) {
                        int index = country.getContinent().getId().intValue() - 1;
                        if (index >= 0 && index < allContinents.size()) {
                            continent = allContinents.get(index);
                        }
                    }
                    if (continent == null) {
                        logger.warn("Continent not found for country '{}' (name={}, id={}), skipping continent reference",
                                country.getName(), country.getContinent().getName(), country.getContinent().getId());
                    }
                    country.setContinent(continent);
                }
                if (country.getEmergencyNumbers() != null) {
                    country.getEmergencyNumbers().forEach(en -> en.setCountry(country));
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
            // Clean up duplicate regions in DB (e.g. from a previous bad seed in prod). Keeps one per (country, name).
            deleteDuplicateRegions();

            Resource resource = getMainResource("static/seedData/regions.json");
            Region[] entities = mapper.readValue(resource.getInputStream(), Region[].class);

            if (entities == null || entities.length == 0) {
                throw new IllegalStateException("No seed data found in regions.json");
            }

            logger.info("Read {} total entities from regions.json", entities.length);

            // Match by (country, name) so "Corsica" in France and "Corsica" elsewhere are different rows
            List<Country> allCountries = countryRepository.findAllByOrderByContinentNameAscNameAsc();
            Map<String, Region> existingMap = regionRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            r -> regionKey(r.getCountry() != null ? r.getCountry().getId() : null, r.getName()),
                            r -> r,
                            (first, second) -> first));

            // Resolve country by name from JSON first so we don't rely on index (avoids wrong country when DB order differs from export)
            List<Region> toSave = Arrays.stream(entities)
                    .map(jsonEntity -> {
                        Long countryId = null;
                        if (jsonEntity.getCountry() != null) {
                            if (jsonEntity.getCountry().getName() != null && !jsonEntity.getCountry().getName().isBlank()) {
                                countryId = countryRepository.findByNameIgnoreCase(jsonEntity.getCountry().getName())
                                        .map(Country::getId).orElse(null);
                                if (countryId == null) {
                                    logger.warn("Country name '{}' not found for region '{}', trying index fallback",
                                            jsonEntity.getCountry().getName(), jsonEntity.getName());
                                }
                            }
                            if (countryId == null && jsonEntity.getCountry().getId() != null) {
                                int index = jsonEntity.getCountry().getId().intValue() - 1;
                                if (index >= 0 && index < allCountries.size()) {
                                    countryId = allCountries.get(index).getId();
                                }
                            }
                        }
                        String key = regionKey(countryId, jsonEntity.getName());
                        Region existing = existingMap.get(key);
                        if (existing != null) {
                            existing.setDescription(jsonEntity.getDescription());
                            existing.setBoundingBox(jsonEntity.getBoundingBox());
                            logger.debug("Updating existing region: {}", existing.getName());
                            return existing;
                        } else {
                            logger.debug("Creating new region: {}", jsonEntity.getName());
                            return jsonEntity;
                        }
                    })
                    .collect(Collectors.toList());

            for (Region region : toSave) {
                if (region.getCountry() != null) {
                    Country country = null;
                    if (region.getCountry().getName() != null && !region.getCountry().getName().isBlank()) {
                        country = countryRepository.findByNameIgnoreCase(region.getCountry().getName()).orElse(null);
                    }
                    if (country == null && region.getCountry().getId() != null) {
                        int index = region.getCountry().getId().intValue() - 1;
                        if (index >= 0 && index < allCountries.size()) {
                            country = allCountries.get(index);
                        }
                    }
                    if (country == null) {
                        logger.warn("Country not found for region '{}' (name={}, id={}), skipping country reference",
                                region.getName(), region.getCountry().getName(), region.getCountry().getId());
                    }
                    region.setCountry(country);
                }
            }

            // Deduplicate by (country, name) so we only save one region per (country, name)
            List<Region> deduplicated = toSave.stream()
                    .collect(Collectors.toMap(
                            r -> regionKey(r.getCountry() != null ? r.getCountry().getId() : null, r.getName()),
                            r -> r,
                            (first, second) -> first))
                    .values().stream()
                    .collect(Collectors.toList());

            saveAndLog(regionRepository, deduplicated, existingMap, r -> regionKey(r.getCountry() != null ? r.getCountry().getId() : null, r.getName()), "regions", "regions.json");
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

            // Match by (region, name) so duplicate names in different regions are different rows
            List<Region> allRegions = regionRepository.findAllByOrderByCountryNameAscNameAsc();
            Map<String, SubRegion> existingMap = subRegionRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            sr -> subRegionKey(sr.getRegion() != null ? sr.getRegion().getId() : null, sr.getName()),
                            sr -> sr,
                            (first, second) -> first));

            List<SubRegion> toSave = Arrays.stream(entities)
                    .map(jsonEntity -> {
                        Long regionId = null;
                        Region resolvedRegion = null;
                        if (jsonEntity.getRegion() != null && jsonEntity.getRegion().getId() != null) {
                            int index = jsonEntity.getRegion().getId().intValue() - 1;
                            if (index >= 0 && index < allRegions.size()) {
                                resolvedRegion = allRegions.get(index);
                                regionId = resolvedRegion.getId();
                            }
                        }
                        String key = subRegionKey(regionId, jsonEntity.getName());
                        SubRegion existing = existingMap.get(key);
                        if (existing != null) {
                            existing.setDescription(jsonEntity.getDescription());
                            if (resolvedRegion != null) {
                                existing.setRegion(resolvedRegion);
                            }
                            logger.debug("Updating existing sub-region: {}", existing.getName());
                            return existing;
                        } else {
                            if (resolvedRegion != null) {
                                jsonEntity.setRegion(resolvedRegion);
                            }
                            logger.debug("Creating new sub-region: {}", jsonEntity.getName());
                            return jsonEntity;
                        }
                    })
                    .collect(Collectors.toList());

            saveAndLog(subRegionRepository, toSave, existingMap, sr -> subRegionKey(sr.getRegion() != null ? sr.getRegion().getId() : null, sr.getName()), "sub-regions", "sub-regions.json");
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
                            existing.setIsRiverWave(jsonEntity.getIsRiverWave());
                            existing.setForecasts(jsonEntity.getForecasts());
                            existing.setWebcams(jsonEntity.getWebcams());
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

            // Resolve region and sub-region references by position (JSON array index = export order)
            List<Region> allRegions = regionRepository.findAllByOrderByCountryNameAscNameAsc();
            List<SubRegion> allSubRegions = subRegionRepository.findAllByOrderByRegionNameAscNameAsc();
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
                // Automatically determine swell season based on coordinates (skip for wavepools and river waves)
                boolean skipSwell = Boolean.TRUE.equals(surfSpot.getIsWavepool()) || Boolean.TRUE.equals(surfSpot.getIsRiverWave());
                if (!skipSwell && surfSpot.getLatitude() != null && surfSpot.getLongitude() != null) {
                    swellSeasonDeterminationService.determineSwellSeason(
                            surfSpot.getLatitude(), 
                            surfSpot.getLongitude()
                    ).ifPresent(surfSpot::setSwellSeason);
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

    /**
     * Removes duplicate regions (same country_id, same name) from the DB, keeping one per (country, name).
     * Reassigns any surf spots that pointed at a removed region to the kept region.
     * Fixes prod DBs that were seeded with duplicate or bad data before we deduplicated in export/seed.
     */
    private void deleteDuplicateRegions() {
        List<Region> all = regionRepository.findAll();
        if (all.isEmpty()) return;

        // Group by (country_id, name); keep region with smallest id per group
        Map<String, Region> keepByKey = new java.util.LinkedHashMap<>();
        List<Region> toDelete = new java.util.ArrayList<>();
        for (Region r : all) {
            Long cid = r.getCountry() != null ? r.getCountry().getId() : null;
            String key = regionKey(cid, r.getName());
            Region kept = keepByKey.get(key);
            if (kept == null) {
                keepByKey.put(key, r);
            } else if (r.getId() != null && kept.getId() != null && r.getId() < kept.getId()) {
                toDelete.add(kept);
                keepByKey.put(key, r);
            } else {
                toDelete.add(r);
            }
        }

        for (Region duplicate : toDelete) {
            Region kept = keepByKey.get(regionKey(duplicate.getCountry() != null ? duplicate.getCountry().getId() : null, duplicate.getName()));
            if (kept == null) continue;
            List<SurfSpot> spots = surfSpotRepository.findByRegion_Id(duplicate.getId());
            for (SurfSpot spot : spots) {
                spot.setRegion(kept);
            }
            surfSpotRepository.saveAll(spots);
            regionRepository.delete(duplicate);
            logger.info("Removed duplicate region: id={}, name='{}', countryId={}", duplicate.getId(), duplicate.getName(), duplicate.getCountry() != null ? duplicate.getCountry().getId() : null);
        }
        if (!toDelete.isEmpty()) {
            regionRepository.flush();
            logger.info("Cleaned up {} duplicate region(s)", toDelete.size());
        }
    }

    private static String regionKey(Long countryId, String name) {
        return (countryId != null ? countryId : 0L) + "|" + (name != null ? name : "");
    }

    private static String subRegionKey(Long regionId, String name) {
        return (regionId != null ? regionId : 0L) + "|" + (name != null ? name : "");
    }

    private <T> void saveAndLog(
            JpaRepository<T, ?> repository,
            List<T> toSave,
            Map<String, T> existingMap,
            Function<T, String> getKeyFunction,
            String entityName,
            String fileName) {
        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
            repository.flush();
            long updated = toSave.stream().filter(e -> existingMap.containsKey(getKeyFunction.apply(e))).count();
            long created = toSave.size() - updated;
            logger.info("Successfully processed {} {} from {} ({} created, {} updated)",
                    toSave.size(), entityName, fileName, created, updated);
        } else {
            logger.info("No entries to process from {}", fileName);
        }
    }
}