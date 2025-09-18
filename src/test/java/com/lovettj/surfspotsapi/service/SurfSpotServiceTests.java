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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;

import java.util.*;

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
    
    private SurfSpotService surfSpotService;

    private String testUserId;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        surfSpotService = new SurfSpotService(surfSpotRepository, regionRepository, subRegionRepository, userSurfSpotService, watchListService);
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

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));

        SurfSpot mockSurfSpot = new SurfSpot();
        mockSurfSpot.setName(request.getName());
        mockSurfSpot.setRegion(mockRegion);
        when(surfSpotRepository.save(any(SurfSpot.class))).thenReturn(mockSurfSpot);

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        verify(surfSpotRepository).save(any(SurfSpot.class));
    }

    @Test
    public void testCreateSurfSpotFailedUserNotFound() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setUserId(null); // Missing user ID
        
        assertThrows(ResponseStatusException.class, () -> surfSpotService.createSurfSpot(request));
    }

    @Test
    void testUpdateSurfSpotShouldReturnUpdatedSurfSpot() {
        SurfSpot updatedSpot = new SurfSpot();
        updatedSpot.setId(1L);

        when(surfSpotRepository.existsById(1L)).thenReturn(true);
        when(surfSpotRepository.save(any(SurfSpot.class))).thenReturn(updatedSpot);

        SurfSpot result = surfSpotService.updateSurfSpot(1L, updatedSpot);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(surfSpotRepository).save(any(SurfSpot.class));
    }

    @Test
    void testUpdateSurfSpotShouldThrowExceptionWhenSurfSpotNotFound() {
        SurfSpot updatedSpot = new SurfSpot();
        updatedSpot.setId(1L);

        when(surfSpotRepository.existsById(1L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> surfSpotService.updateSurfSpot(1L, updatedSpot));
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
}
