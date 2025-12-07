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
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

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
        seedEntities("swell-seasons.json", SwellSeason[].class, swellSeasonRepository.findAll(), swellSeasonRepository::saveAll,
                SwellSeason::getName);
        swellSeasonRepository.flush();
    }

    @Transactional
    public void seedContinents() {
        seedEntities("continents.json", Continent[].class, continentRepository.findAll(), continentRepository::saveAll,
                Continent::getName);
        continentRepository.flush();
        logFirstIds(continentRepository.findAll().stream().map(Continent::getId).toList(), "continents");
    }

    @Transactional
    public void seedCountries() {
        try {
            ClassPathResource resource = new ClassPathResource("static/seedData/countries.json");
            Country[] entities = mapper.readValue(resource.getInputStream(), Country[].class);

            if (entities == null || entities.length == 0) {
                throw new IllegalStateException("No seed data found in countries.json");
            }

            logger.info("Read {} total entities from countries.json", entities.length);

            List<String> existingEntityNames = countryRepository.findAll().stream()
                    .map(Country::getName)
                    .collect(Collectors.toList());

            List<Country> newEntities = Arrays.stream(entities)
                    .filter(entity -> !existingEntityNames.contains(entity.getName()))
                    .collect(Collectors.toList());

            // Load all continents once to resolve references by position
            List<Continent> allContinents = continentRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());

            // Resolve continent references by position (JSON ID - 1 = 0-based index)
            for (Country country : newEntities) {
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

            if (!newEntities.isEmpty()) {
                countryRepository.saveAll(newEntities);
                logger.info("Successfully seeded {} entries from countries.json", newEntities.size());
            } else {
                logger.info("No new entries to seed from countries.json - all {} entities already exist", entities.length);
            }

            countryRepository.flush();
            logFirstIds(countryRepository.findAll().stream().map(Country::getId).toList(), "countries");
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from countries.json: {}", e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from countries.json: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void seedRegions() {
        try {
            ClassPathResource resource = new ClassPathResource("static/seedData/regions.json");
            Region[] entities = mapper.readValue(resource.getInputStream(), Region[].class);

            if (entities == null || entities.length == 0) {
                throw new IllegalStateException("No seed data found in regions.json");
            }

            logger.info("Read {} total entities from regions.json", entities.length);

            List<String> existingEntityNames = regionRepository.findAll().stream()
                    .map(Region::getName)
                    .collect(Collectors.toList());

            List<Region> newEntities = Arrays.stream(entities)
                    .filter(entity -> !existingEntityNames.contains(entity.getName()))
                    .collect(Collectors.toList());

            // Load all countries once to resolve references by position
            List<Country> allCountries = countryRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());

            // Resolve country references by position (JSON ID - 1 = 0-based index)
            for (Region region : newEntities) {
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

            if (!newEntities.isEmpty()) {
                regionRepository.saveAll(newEntities);
                logger.info("Successfully seeded {} entries from regions.json", newEntities.size());
            } else {
                logger.info("No new entries to seed from regions.json - all {} entities already exist", entities.length);
            }

            regionRepository.flush();
            logFirstIds(regionRepository.findAll().stream().map(Region::getId).toList(), "regions");
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from regions.json: {}", e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from regions.json: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void seedSubRegions() {
        try {
            ClassPathResource resource = new ClassPathResource("static/seedData/sub-regions.json");
            SubRegion[] entities = mapper.readValue(resource.getInputStream(), SubRegion[].class);

            if (entities == null || entities.length == 0) {
                throw new IllegalStateException("No seed data found in sub-regions.json");
            }

            logger.info("Read {} total entities from sub-regions.json", entities.length);

            List<String> existingEntityNames = subRegionRepository.findAll().stream()
                    .map(SubRegion::getName)
                    .collect(Collectors.toList());

            List<SubRegion> newEntities = Arrays.stream(entities)
                    .filter(entity -> !existingEntityNames.contains(entity.getName()))
                    .collect(Collectors.toList());

            // Load all regions once to resolve references by position
            List<Region> allRegions = regionRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());

            // Resolve region references by position (JSON ID - 1 = 0-based index)
            for (SubRegion subRegion : newEntities) {
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

            if (!newEntities.isEmpty()) {
                subRegionRepository.saveAll(newEntities);
                logger.info("Successfully seeded {} entries from sub-regions.json", newEntities.size());
            } else {
                logger.info("No new entries to seed from sub-regions.json - all {} entities already exist", entities.length);
            }

            subRegionRepository.flush();
            logFirstIds(subRegionRepository.findAll().stream().map(SubRegion::getId).toList(), "sub-regions");
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from sub-regions.json: {}", e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from sub-regions.json: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void seedSurfSpots() {
        try {
            ClassPathResource resource = new ClassPathResource("static/seedData/surf-spots.json");
            SurfSpot[] entities = mapper.readValue(resource.getInputStream(), SurfSpot[].class);

            if (entities == null || entities.length == 0) {
                throw new IllegalStateException("No seed data found in surf-spots.json");
            }

            logger.info("Read {} total entities from surf-spots.json", entities.length);

            List<String> existingEntityNames = surfSpotRepository.findAll().stream()
                    .map(SurfSpot::getName)
                    .collect(Collectors.toList());

            List<SurfSpot> newEntities = Arrays.stream(entities)
                    .filter(entity -> !existingEntityNames.contains(entity.getName()))
                    .collect(Collectors.toList());

            // Load all regions and sub-regions once to avoid N+1 query problem
            // Regions are ordered by ID, and JSON region IDs represent position in seeded list
            List<Region> allRegions = regionRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());
            List<SubRegion> allSubRegions = subRegionRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .collect(Collectors.toList());

            // Resolve region and sub-region references by position in seeded list
            // JSON IDs represent the order in which entities were seeded (1-based index)
            for (SurfSpot surfSpot : newEntities) {
                // Resolve region reference by position (JSON ID - 1 = 0-based index)
                if (surfSpot.getRegion() != null && surfSpot.getRegion().getId() != null) {
                    Long jsonRegionId = surfSpot.getRegion().getId();
                    int index = jsonRegionId.intValue() - 1; // Convert to 0-based index
                    if (index >= 0 && index < allRegions.size()) {
                        surfSpot.setRegion(allRegions.get(index));
                    } else {
                        logger.warn("Region with JSON id {} (index {}) not found for surf spot '{}', skipping region reference. Total regions: {}", 
                                jsonRegionId, index, surfSpot.getName(), allRegions.size());
                        surfSpot.setRegion(null);
                    }
                }
                
                // Resolve sub-region reference by position
                if (surfSpot.getSubRegion() != null && surfSpot.getSubRegion().getId() != null) {
                    Long jsonSubRegionId = surfSpot.getSubRegion().getId();
                    int index = jsonSubRegionId.intValue() - 1; // Convert to 0-based index
                    if (index >= 0 && index < allSubRegions.size()) {
                        surfSpot.setSubRegion(allSubRegions.get(index));
                    } else {
                        logger.warn("SubRegion with JSON id {} (index {}) not found for surf spot '{}', skipping sub-region reference. Total sub-regions: {}", 
                                jsonSubRegionId, index, surfSpot.getName(), allSubRegions.size());
                        surfSpot.setSubRegion(null);
                    }
                }
                
                // Automatically determine swell season for each surf spot based on coordinates
                // Skip for wavepools as they don't have natural swell seasons
                if (surfSpot.getIsWavepool() == null || !surfSpot.getIsWavepool()) {
                    if (surfSpot.getLatitude() != null && surfSpot.getLongitude() != null) {
                        swellSeasonDeterminationService.determineSwellSeason(
                                surfSpot.getLatitude(), 
                                surfSpot.getLongitude()
                        ).ifPresent(surfSpot::setSwellSeason);
                    }
                }
            }

            if (!newEntities.isEmpty()) {
                surfSpotRepository.saveAll(newEntities);
                logger.info("Successfully seeded {} entries from surf-spots.json", newEntities.size());
            } else {
                logger.info("No new entries to seed from surf-spots.json - all {} entities already exist", entities.length);
            }
            
            surfSpotRepository.flush();
            logFirstIds(surfSpotRepository.findAll().stream().map(SurfSpot::getId).toList(), "surf-spots");
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from surf-spots.json: {}", e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from surf-spots.json: {}", e.getMessage(), e);
        }
    }

    private <T> void seedEntities(
            String fileName,
            Class<T[]> entityType,
            List<T> existingEntities,
            java.util.function.Consumer<List<T>> saveAll,
            java.util.function.Function<T, String> getNameFunction
    ) {
        try {
            ClassPathResource resource = new ClassPathResource("static/seedData/" + fileName);
            T[] entities = mapper.readValue(resource.getInputStream(), entityType);

            if (entities == null || entities.length == 0) {
                throw new IllegalStateException("No seed data found in " + fileName);
            }

            logger.info("Read {} total entities from {}", entities.length, fileName);

            List<String> existingEntityNames = existingEntities.stream()
                    .map(getNameFunction)
                    .collect(Collectors.toList());

            // Filter to only new entities BEFORE creating the list - this ensures we only keep
            // entities we're actually going to save, preventing transient entities from staying in session
            List<T> newEntities = Arrays.stream(entities)
                    .filter(entity -> !existingEntityNames.contains(getNameFunction.apply(entity)))
                    .collect(Collectors.toList());

            if (!newEntities.isEmpty()) {
                saveAll.accept(newEntities);
                logger.info("Successfully seeded {} entries from {}", newEntities.size(), fileName);
            } else {
                logger.info("No new entries to seed from {} - all {} entities already exist", fileName, entities.length);
            }
        } catch (IOException e) {
            logger.error("Failed to read or parse seed data from {}: {}", fileName, e.getMessage(), e);
        } catch (DataAccessException e) {
            logger.error("Database access error while seeding from {}: {}", fileName, e.getMessage(), e);
        }
    }

    private void logFirstIds(List<Long> ids, String entityName) {
        logger.info("First 5 {} IDs after flush: {}", entityName, ids.stream().limit(5).toList());
    }
}