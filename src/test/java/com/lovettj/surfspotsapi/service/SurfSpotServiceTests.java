package com.lovettj.surfspotsapi.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.entity.*;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SubRegionRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.requests.BoundingBox;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class SurfSpotServiceTests {

    @Mock
    private SurfSpotRepository surfSpotRepository;
    
    @Mock
    private RegionRepository regionRepository;
    
    @Mock
    private SubRegionRepository subRegionRepository;
    
    @Mock
    private UserSurfSpotService userSurfSpotService;
    
    @Mock
    private WatchListService watchListService;
    
    @Mock
    private SwellSeasonDeterminationService swellSeasonDeterminationService;
    
    private SurfSpotService surfSpotService;

    private String testUserId;

    @BeforeEach
    public void setUp() {
        surfSpotService = new SurfSpotService(surfSpotRepository, regionRepository, subRegionRepository, userSurfSpotService, watchListService, swellSeasonDeterminationService);
        testUserId = "test-user-id-123";
    }

    // Helper to create a fully populated mock Region (with Country and Continent)
    private Region createMockRegion() {
        Continent continent = new Continent();
        continent.setName("Africa");
        continent.setSlug("africa");
        Country country = new Country();
        country.setName("Morocco");
        country.setSlug("morocco");
        country.setContinent(continent);
        Region region = new Region();
        region.setName("Taghazout");
        region.setSlug("taghazout");
        region.setCountry(country);
        return region;
    }

    // Helper to create a fully populated mock SurfSpot (with Region, Country, Continent)
    private SurfSpot createMockSurfSpot() {
        Region region = createMockRegion();
        SurfSpot spot = new SurfSpot();
        spot.setName("TestSpot");
        spot.setRegion(region);
        spot.setSlug("test-spot");
        spot.setId(1L);
        return spot;
    }

    @Test
    public void testFindSurfSpotsWithinBoundsWithFilters() {
        BoundingBox boundingBox = new BoundingBox(10.0, 20.0, 30.0, 40.0);
        SurfSpotBoundsFilterDTO filters = new SurfSpotBoundsFilterDTO();
        filters.setMinLatitude(10.0);
        filters.setMaxLatitude(20.0);
        filters.setMinLongitude(30.0);
        filters.setMaxLongitude(40.0);
        filters.setUserId(testUserId);
        SurfSpot spot1 = createMockSurfSpot();
        SurfSpot spot2 = createMockSurfSpot();
        List<SurfSpot> mockSurfSpots = Arrays.asList(spot1, spot2);
        when(surfSpotRepository.findWithinBoundsWithFilters(filters)).thenReturn(mockSurfSpots);
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsWithinBoundsWithFilters(boundingBox, filters);
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(surfSpotRepository).findWithinBoundsWithFilters(filters);
    }

    @Test
    public void testFindSurfSpotsByRegionSlugWithFilters() {
        String regionSlug = "region-slug";
        Region mockRegion = createMockRegion();
        List<SurfSpot> mockSurfSpots = Collections.singletonList(createMockSurfSpot());
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        when(regionRepository.findBySlug(regionSlug)).thenReturn(Optional.of(mockRegion));
        when(surfSpotRepository.findByRegionWithFilters(mockRegion, filters)).thenReturn(mockSurfSpots);
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlugWithFilters(regionSlug, filters);
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(regionRepository).findBySlug(regionSlug);
        verify(surfSpotRepository).findByRegionWithFilters(mockRegion, filters);
    }

    @Test
    public void testCreateSurfSpot() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Test Spot");
        request.setDescription("Great surf spot!");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270); // Cadiz, Spain - Atlantic coast
        request.setLongitude(-6.2886);

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));

        SwellSeason mockSwellSeason = new SwellSeason();
        mockSwellSeason.setId(1L);
        mockSwellSeason.setName("North Atlantic");
        mockSwellSeason.setStartMonth("September");
        mockSwellSeason.setEndMonth("April");
        when(swellSeasonDeterminationService.determineSwellSeason(36.5270, -6.2886))
                .thenReturn(Optional.of(mockSwellSeason));

        SurfSpot mockSurfSpot = new SurfSpot();
        mockSurfSpot.setName(request.getName());
        mockSurfSpot.setRegion(mockRegion);
        mockSurfSpot.setSwellSeason(mockSwellSeason);
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertNotNull(result.getSwellSeason());
        assertEquals("North Atlantic", result.getSwellSeason().getName());
        verify(surfSpotRepository).save(any(SurfSpot.class));
        verify(swellSeasonDeterminationService).determineSwellSeason(36.5270, -6.2886);
    }

    @Test
    public void testCreateSurfSpotShouldAutomaticallySetSwellSeasonForMediterranean() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Mediterranean Spot");
        request.setDescription("Mediterranean surf spot");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.7213); // Malaga, Spain - Mediterranean coast
        request.setLongitude(-4.4214);

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));

        SwellSeason mockSwellSeason = new SwellSeason();
        mockSwellSeason.setId(2L);
        mockSwellSeason.setName("Mediterranean");
        mockSwellSeason.setStartMonth("October");
        mockSwellSeason.setEndMonth("March");
        when(swellSeasonDeterminationService.determineSwellSeason(36.7213, -4.4214))
                .thenReturn(Optional.of(mockSwellSeason));

        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertNotNull(result);
        assertNotNull(result.getSwellSeason());
        assertEquals("Mediterranean", result.getSwellSeason().getName());
        verify(swellSeasonDeterminationService).determineSwellSeason(36.7213, -4.4214);
    }

    @Test
    public void testCreateSurfSpotShouldNotSetSwellSeasonWhenCoordinatesNotDetermined() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Unknown Region Spot");
        request.setDescription("Spot in unknown region");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(0.0); // Middle of ocean - no known region
        request.setLongitude(0.0);

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));

        when(swellSeasonDeterminationService.determineSwellSeason(0.0, 0.0))
                .thenReturn(Optional.empty());

        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertNotNull(result);
        assertNull(result.getSwellSeason()); // No swell season when not determined
        verify(swellSeasonDeterminationService).determineSwellSeason(0.0, 0.0);
    }

    @Test
    public void testCreateSurfSpotFailedUserNotFound() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setUserId(null); // Missing user ID
        
        assertThrows(ResponseStatusException.class, () -> surfSpotService.createSurfSpot(request));
    }

    @Test
    void testUpdateSurfSpotShouldReturnUpdatedSurfSpot() {
        SurfSpot existingSpot = new SurfSpot();
        existingSpot.setId(1L);
        existingSpot.setName("Original Name");
        existingSpot.setLatitude(36.5270);
        existingSpot.setLongitude(-6.2886);

        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");
        request.setUserId("test-user-id");
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);

        SwellSeason mockSwellSeason = new SwellSeason();
        mockSwellSeason.setId(1L);
        mockSwellSeason.setName("North Atlantic");
        when(swellSeasonDeterminationService.determineSwellSeason(36.5270, -6.2886))
                .thenReturn(Optional.of(mockSwellSeason));

        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));
        when(surfSpotRepository.save(any(SurfSpot.class))).thenReturn(existingSpot);

        SurfSpot result = surfSpotService.updateSurfSpot(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertNotNull(result.getSwellSeason());
        verify(surfSpotRepository).findById(1L);
        verify(surfSpotRepository).save(any(SurfSpot.class));
        verify(swellSeasonDeterminationService).determineSwellSeason(36.5270, -6.2886);
    }

    @Test
    void testUpdateSurfSpotShouldUpdateSwellSeasonWhenCoordinatesChange() {
        SurfSpot existingSpot = new SurfSpot();
        existingSpot.setId(1L);
        existingSpot.setName("Original Spot");
        existingSpot.setLatitude(36.5270); // Atlantic coast
        existingSpot.setLongitude(-6.2886);
        
        SwellSeason oldSwellSeason = new SwellSeason();
        oldSwellSeason.setName("North Atlantic");
        existingSpot.setSwellSeason(oldSwellSeason);

        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Updated Spot");
        request.setUserId("test-user-id");
        request.setLatitude(36.7213); // Mediterranean coast - different swell season
        request.setLongitude(-4.4214);

        SwellSeason newSwellSeason = new SwellSeason();
        newSwellSeason.setId(2L);
        newSwellSeason.setName("Mediterranean");
        when(swellSeasonDeterminationService.determineSwellSeason(36.7213, -4.4214))
                .thenReturn(Optional.of(newSwellSeason));

        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SurfSpot result = surfSpotService.updateSurfSpot(1L, request);

        assertNotNull(result);
        assertNotNull(result.getSwellSeason());
        assertEquals("Mediterranean", result.getSwellSeason().getName());
        verify(swellSeasonDeterminationService).determineSwellSeason(36.7213, -4.4214);
    }

    @Test
    void testUpdateSurfSpotShouldThrowExceptionWhenSurfSpotNotFound() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Updated Name");
        request.setUserId("test-user-id");

        when(surfSpotRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> surfSpotService.updateSurfSpot(1L, request));
    }

    @Test
    public void testDeleteSurfSpot() {
        Long surfSpotId = 1L;
        
        doNothing().when(surfSpotRepository).deleteById(surfSpotId);

        surfSpotService.deleteSurfSpot(surfSpotId);

        verify(surfSpotRepository).deleteById(surfSpotId);
    }

    @Test
    void testFindSurfSpotsBySubRegionSlugWithFiltersShouldReturnSurfSpotsWhenSubRegionExists() {
        // Arrange
        String subRegionSlug = "test-sub-region";
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setUserId("user123");
        
        Region region = createMockRegion();
        
        SubRegion subRegion = SubRegion.builder()
                .id(1L)
                .name("Test Sub-Region")
                .description("Test sub-region description")
                .region(region)
                .build();
        subRegion.generateSlug();
        
        SurfSpot surfSpot1 = SurfSpot.builder()
                .id(1L)
                .name("Test Surf Spot 1")
                .description("Test surf spot 1 description")
                .region(region)
                .subRegion(subRegion)
                .build();
        surfSpot1.generateSlug();
        
        SurfSpot surfSpot2 = SurfSpot.builder()
                .id(2L)
                .name("Test Surf Spot 2")
                .description("Test surf spot 2 description")
                .region(region)
                .subRegion(subRegion)
                .build();
        surfSpot2.generateSlug();
        
        List<SurfSpot> surfSpots = Arrays.asList(surfSpot1, surfSpot2);
        
        when(subRegionRepository.findBySlug(subRegionSlug)).thenReturn(Optional.of(subRegion));
        when(surfSpotRepository.findBySubRegionWithFilters(subRegion, filters)).thenReturn(surfSpots);
        when(userSurfSpotService.isUserSurfedSpot("user123", 1L)).thenReturn(false);
        when(userSurfSpotService.isUserSurfedSpot("user123", 2L)).thenReturn(true);
        when(watchListService.isWatched("user123", 1L)).thenReturn(false);
        when(watchListService.isWatched("user123", 2L)).thenReturn(false);

        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsBySubRegionSlugWithFilters(subRegionSlug, filters);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(surfSpot1.getName(), result.get(0).getName());
        assertEquals(surfSpot2.getName(), result.get(1).getName());
        assertFalse(result.get(0).getIsSurfedSpot());
        assertTrue(result.get(1).getIsSurfedSpot());
        verify(subRegionRepository).findBySlug(subRegionSlug);
        verify(surfSpotRepository).findBySubRegionWithFilters(subRegion, filters);
    }

    @Test
    void testFindSurfSpotsBySubRegionSlugWithFiltersShouldThrowExceptionWhenSubRegionDoesNotExist() {
        // Arrange
        String subRegionSlug = "non-existent-sub-region";
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        
        when(subRegionRepository.findBySlug(subRegionSlug)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> surfSpotService.findSurfSpotsBySubRegionSlugWithFilters(subRegionSlug, filters));
        assertEquals("SubRegion not found", exception.getMessage());
        verify(subRegionRepository).findBySlug(subRegionSlug);
        verify(surfSpotRepository, never()).findBySubRegionWithFilters(any(), any());
    }

    @Test
    void testFindSurfSpotsBySubRegionSlugWithFiltersShouldReturnEmptyListWhenSubRegionHasNoSurfSpots() {
        // Arrange
        String subRegionSlug = "test-sub-region";
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setUserId("user123");
        
        Region region = createMockRegion();
        
        SubRegion subRegion = SubRegion.builder()
                .id(1L)
                .name("Test Sub-Region")
                .description("Test sub-region description")
                .region(region)
                .build();
        subRegion.generateSlug();
        
        when(subRegionRepository.findBySlug(subRegionSlug)).thenReturn(Optional.of(subRegion));
        when(surfSpotRepository.findBySubRegionWithFilters(subRegion, filters)).thenReturn(Arrays.asList());

        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsBySubRegionSlugWithFilters(subRegionSlug, filters);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(subRegionRepository).findBySlug(subRegionSlug);
        verify(surfSpotRepository).findBySubRegionWithFilters(subRegion, filters);
    }

    @Test
    void testFindSurfSpotsBySubRegionSlugWithFiltersShouldGenerateCorrectPathsWhenSurfSpotsHaveSubRegions() {
        // Arrange
        String subRegionSlug = "test-sub-region";
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setUserId("user123");
        
        Continent continent = Continent.builder()
                .id(1L)
                .name("North America")
                .build();
        continent.generateSlug();
        
        Country country = Country.builder()
                .id(1L)
                .name("United States")
                .continent(continent)
                .build();
        country.generateSlug();
        
        Region region = Region.builder()
                .id(1L)
                .name("California")
                .country(country)
                .build();
        region.generateSlug();
        
        SubRegion subRegion = SubRegion.builder()
                .id(1L)
                .name("Test Sub-Region")
                .description("Test sub-region description")
                .region(region)
                .build();
        subRegion.generateSlug();
        
        SurfSpot surfSpot1 = SurfSpot.builder()
                .id(1L)
                .name("Test Surf Spot 1")
                .description("Test surf spot 1 description")
                .region(region)
                .subRegion(subRegion)
                .build();
        surfSpot1.generateSlug();
        
        List<SurfSpot> surfSpots = Arrays.asList(surfSpot1);
        
        when(subRegionRepository.findBySlug(subRegionSlug)).thenReturn(Optional.of(subRegion));
        when(surfSpotRepository.findBySubRegionWithFilters(subRegion, filters)).thenReturn(surfSpots);
        when(userSurfSpotService.isUserSurfedSpot("user123", 1L)).thenReturn(false);
        when(watchListService.isWatched("user123", 1L)).thenReturn(false);

        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsBySubRegionSlugWithFilters(subRegionSlug, filters);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        String expectedPath = String.format("/surf-spots/%s/%s/%s/sub-regions/%s/%s",
                continent.getSlug(),
                country.getSlug(),
                region.getSlug(),
                subRegion.getSlug(),
                surfSpot1.getSlug());
        assertEquals(expectedPath, result.get(0).getPath());
        verify(subRegionRepository).findBySlug(subRegionSlug);
        verify(surfSpotRepository).findBySubRegionWithFilters(subRegion, filters);
    }

    @Test
    void testFindSurfSpotsByRegionSlugWithFiltersShouldGenerateCorrectPathsWhenSurfSpotsHaveNoSubRegions() {
        // Arrange
        String regionSlug = "test-region";
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setUserId("user123");
        
        Continent continent = Continent.builder()
                .id(1L)
                .name("North America")
                .build();
        continent.generateSlug();
        
        Country country = Country.builder()
                .id(1L)
                .name("United States")
                .continent(continent)
                .build();
        country.generateSlug();
        
        Region region = Region.builder()
                .id(1L)
                .name("Test Region")
                .country(country)
                .build();
        region.generateSlug();
        
        SurfSpot surfSpot1 = SurfSpot.builder()
                .id(1L)
                .name("Test Surf Spot 1")
                .description("Test surf spot 1 description")
                .region(region)
                .subRegion(null) // No sub-region
                .build();
        surfSpot1.generateSlug();
        
        List<SurfSpot> surfSpots = Arrays.asList(surfSpot1);
        
        when(regionRepository.findBySlug(regionSlug)).thenReturn(Optional.of(region));
        when(surfSpotRepository.findByRegionWithFilters(region, filters)).thenReturn(surfSpots);
        when(userSurfSpotService.isUserSurfedSpot("user123", 1L)).thenReturn(false);
        when(watchListService.isWatched("user123", 1L)).thenReturn(false);

        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlugWithFilters(regionSlug, filters);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        String expectedPath = String.format("/surf-spots/%s/%s/%s/%s",
                continent.getSlug(),
                country.getSlug(),
                region.getSlug(),
                surfSpot1.getSlug());
        assertEquals(expectedPath, result.get(0).getPath());
        verify(regionRepository).findBySlug(regionSlug);
        verify(surfSpotRepository).findByRegionWithFilters(region, filters);
    }

    @Test
    void testFindSurfSpotsByRegionSlugWithFiltersShouldFilterBySeasonWhenNormalRange() {
        // Arrange
        SwellSeason season1 = new SwellSeason();
        season1.setId(1L);
        season1.setName("Test Season 1");
        season1.setStartMonth("March");
        season1.setEndMonth("June");
        
        SwellSeason season2 = new SwellSeason();
        season2.setId(2L);
        season2.setName("Test Season 2");
        season2.setStartMonth("July");
        season2.setEndMonth("September");
        
        SurfSpot spot1 = createMockSurfSpot();
        spot1.setSwellSeason(season1);
        
        SurfSpot spot2 = createMockSurfSpot();
        spot2.setSwellSeason(season2);
        
        List<SurfSpot> surfSpots = Arrays.asList(spot1, spot2);
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setSeasons(Arrays.asList("april", "may")); // Should match spot1
        
        when(regionRepository.findBySlug(any())).thenReturn(Optional.of(createMockRegion()));
        when(surfSpotRepository.findByRegionWithFilters(any(), any())).thenReturn(surfSpots);
        
        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlugWithFilters("test-region", filters);
        
        // Assert - spot1 should be included (april and may are in march-june range)
        assertEquals(1, result.size());
        assertEquals(spot1.getName(), result.get(0).getName());
    }

    @Test
    void testFindSurfSpotsByRegionSlugWithFiltersShouldFilterBySeasonWhenWrappingRange() {
        // Arrange
        SwellSeason season1 = new SwellSeason();
        season1.setId(1L);
        season1.setName("Test Season 1");
        season1.setStartMonth("December");
        season1.setEndMonth("April"); // Wrapping range
        
        SwellSeason season2 = new SwellSeason();
        season2.setId(2L);
        season2.setName("Test Season 2");
        season2.setStartMonth("May");
        season2.setEndMonth("August");
        
        SurfSpot spot1 = createMockSurfSpot();
        spot1.setSwellSeason(season1);
        
        SurfSpot spot2 = createMockSurfSpot();
        spot2.setSwellSeason(season2);
        
        List<SurfSpot> surfSpots = Arrays.asList(spot1, spot2);
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setSeasons(Arrays.asList("january", "february")); // Should match spot1 (wrapping range)
        
        when(regionRepository.findBySlug(any())).thenReturn(Optional.of(createMockRegion()));
        when(surfSpotRepository.findByRegionWithFilters(any(), any())).thenReturn(surfSpots);
        
        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlugWithFilters("test-region", filters);
        
        // Assert - spot1 should be included (january and february are in december-april wrapping range)
        assertEquals(1, result.size());
        assertEquals(spot1.getName(), result.get(0).getName());
    }

    @Test
    void testFindSurfSpotsByRegionSlugWithFiltersShouldReturnEmptyListWhenNoSeasonMatch() {
        // Arrange
        SwellSeason season1 = new SwellSeason();
        season1.setId(1L);
        season1.setName("Test Season 1");
        season1.setStartMonth("March");
        season1.setEndMonth("June");
        
        SurfSpot spot1 = createMockSurfSpot();
        spot1.setSwellSeason(season1);
        
        List<SurfSpot> surfSpots = Arrays.asList(spot1);
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setSeasons(Arrays.asList("july", "august")); // Should not match spot1
        
        when(regionRepository.findBySlug(any())).thenReturn(Optional.of(createMockRegion()));
        when(surfSpotRepository.findByRegionWithFilters(any(), any())).thenReturn(surfSpots);
        
        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlugWithFilters("test-region", filters);
        
        // Assert - no spots should match
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindSurfSpotsByRegionSlugWithFiltersShouldFilterBySeasonWhenMultipleSelectedMonths() {
        // Arrange
        SwellSeason season1 = new SwellSeason();
        season1.setId(1L);
        season1.setName("Test Season 1");
        season1.setStartMonth("March");
        season1.setEndMonth("June");
        
        SwellSeason season2 = new SwellSeason();
        season2.setId(2L);
        season2.setName("Test Season 2");
        season2.setStartMonth("July");
        season2.setEndMonth("September");
        
        SurfSpot spot1 = createMockSurfSpot();
        spot1.setSwellSeason(season1);
        
        SurfSpot spot2 = createMockSurfSpot();
        spot2.setSwellSeason(season2);
        
        List<SurfSpot> surfSpots = Arrays.asList(spot1, spot2);
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setSeasons(Arrays.asList("april", "august")); // april matches spot1, august matches spot2
        
        when(regionRepository.findBySlug(any())).thenReturn(Optional.of(createMockRegion()));
        when(surfSpotRepository.findByRegionWithFilters(any(), any())).thenReturn(surfSpots);
        
        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlugWithFilters("test-region", filters);
        
        // Assert - both spots should match
        assertEquals(2, result.size());
    }

    @Test
    void testFindSurfSpotsByRegionSlugWithFiltersShouldExcludeSpotsWhenSwellSeasonIsNull() {
        // Arrange
        SurfSpot spot1 = createMockSurfSpot();
        spot1.setSwellSeason(null); // No swell season
        
        SurfSpot spot2 = createMockSurfSpot();
        SwellSeason season2 = new SwellSeason();
        season2.setId(2L);
        season2.setName("Test Season");
        season2.setStartMonth("July");
        season2.setEndMonth("September");
        spot2.setSwellSeason(season2);
        
        List<SurfSpot> surfSpots = Arrays.asList(spot1, spot2);
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setSeasons(Arrays.asList("april"));
        
        when(regionRepository.findBySlug(any())).thenReturn(Optional.of(createMockRegion()));
        when(surfSpotRepository.findByRegionWithFilters(any(), any())).thenReturn(surfSpots);
        
        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlugWithFilters("test-region", filters);
        
        // Assert - spot1 (null swell season) should be excluded, spot2 doesn't match april
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindSurfSpotsByRegionSlugWithFiltersShouldFilterWavepoolsBySeason() {
        // Arrange
        SwellSeason season1 = new SwellSeason();
        season1.setId(1L);
        season1.setName("Test Season 1");
        season1.setStartMonth("May");
        season1.setEndMonth("September");
        
        SwellSeason season2 = new SwellSeason();
        season2.setId(2L);
        season2.setName("Test Season 2");
        season2.setStartMonth("March");
        season2.setEndMonth("June");
        
        SurfSpot wavepool = createMockSurfSpot();
        wavepool.setSwellSeason(season1);
        wavepool.setIsWavepool(true); // Wavepool with seasons
        
        SurfSpot regularSpot = createMockSurfSpot();
        regularSpot.setSwellSeason(season2);
        regularSpot.setIsWavepool(false);
        
        List<SurfSpot> surfSpots = Arrays.asList(wavepool, regularSpot);
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setSeasons(Arrays.asList("april"));
        
        when(regionRepository.findBySlug(any())).thenReturn(Optional.of(createMockRegion()));
        when(surfSpotRepository.findByRegionWithFilters(any(), any())).thenReturn(surfSpots);
        
        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlugWithFilters("test-region", filters);
        
        // Assert - regular spot should match april, wavepool should not (april is not in may-september)
        assertEquals(1, result.size());
        assertEquals(regularSpot.getName(), result.get(0).getName());
    }

    @Test
    void testFindSurfSpotsByRegionSlugWithFiltersShouldReturnAllSpotsWhenSeasonFilterIsEmpty() {
        // Arrange
        SwellSeason season1 = new SwellSeason();
        season1.setId(1L);
        season1.setName("Test Season 1");
        season1.setStartMonth("March");
        season1.setEndMonth("June");
        
        SurfSpot spot1 = createMockSurfSpot();
        spot1.setSwellSeason(season1);
        
        List<SurfSpot> surfSpots = Arrays.asList(spot1);
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setSeasons(Collections.emptyList()); // Empty filter
        
        when(regionRepository.findBySlug(any())).thenReturn(Optional.of(createMockRegion()));
        when(surfSpotRepository.findByRegionWithFilters(any(), any())).thenReturn(surfSpots);
        
        // Act
        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlugWithFilters("test-region", filters);
        
        // Assert - all spots should be returned when filter is empty
        assertEquals(1, result.size());
    }

    @Test
    void testCreateSurfSpotShouldDetermineCorrectSwellSeasonForRegionWithMultipleCoastlines() {
        // Test the edge case where a region (like Andalusia) has multiple coastlines
        // Atlantic coast should get North Atlantic, Mediterranean coast should get Mediterranean
        
        // Test Atlantic coast (Cadiz, Spain)
        SurfSpotRequest atlanticRequest = new SurfSpotRequest();
        atlanticRequest.setName("Atlantic Coast Spot");
        atlanticRequest.setUserId(testUserId);
        atlanticRequest.setRegionId(1L);
        atlanticRequest.setLatitude(36.5270); // Cadiz - Atlantic coast
        atlanticRequest.setLongitude(-6.2886);

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));

        SwellSeason atlanticSeason = new SwellSeason();
        atlanticSeason.setId(1L);
        atlanticSeason.setName("North Atlantic");
        atlanticSeason.setStartMonth("September");
        atlanticSeason.setEndMonth("April");
        when(swellSeasonDeterminationService.determineSwellSeason(36.5270, -6.2886))
                .thenReturn(Optional.of(atlanticSeason));

        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot atlanticResult = surfSpotService.createSurfSpot(atlanticRequest);
        assertEquals("North Atlantic", atlanticResult.getSwellSeason().getName());

        // Test Mediterranean coast (Malaga, Spain)
        SurfSpotRequest medRequest = new SurfSpotRequest();
        medRequest.setName("Mediterranean Coast Spot");
        medRequest.setUserId(testUserId);
        medRequest.setRegionId(1L);
        medRequest.setLatitude(36.7213); // Malaga - Mediterranean coast
        medRequest.setLongitude(-4.4214);

        SwellSeason medSeason = new SwellSeason();
        medSeason.setId(2L);
        medSeason.setName("Mediterranean");
        medSeason.setStartMonth("October");
        medSeason.setEndMonth("March");
        when(swellSeasonDeterminationService.determineSwellSeason(36.7213, -4.4214))
                .thenReturn(Optional.of(medSeason));

        SurfSpot medResult = surfSpotService.createSurfSpot(medRequest);
        assertEquals("Mediterranean", medResult.getSwellSeason().getName());

        // Verify both determinations were called
        verify(swellSeasonDeterminationService).determineSwellSeason(36.5270, -6.2886);
        verify(swellSeasonDeterminationService).determineSwellSeason(36.7213, -4.4214);
    }
}
