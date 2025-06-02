package com.lovettj.surfspotsapi.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.*;
import com.lovettj.surfspotsapi.repository.RegionRepository;
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

public class SurfSpotServiceTests {

    @Mock
    private SurfSpotRepository surfSpotRepository;
    
    @Mock
    private RegionRepository regionRepository;
    
    @Mock
    private UserSurfSpotService userSurfSpotService;
    
    @Mock
    private WatchListService watchListService;
    
    private SurfSpotService surfSpotService;

    private String testUserId;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        surfSpotService = new SurfSpotService(surfSpotRepository, regionRepository, userSurfSpotService, watchListService);
        testUserId = "test-user-id-123";
    }

    @Test
    public void testGetAllSurfSpots() {
        List<SurfSpot> mockSurfSpots = Arrays.asList(new SurfSpot(), new SurfSpot());
        when(surfSpotRepository.findAll()).thenReturn(mockSurfSpots);

        List<SurfSpot> result = surfSpotService.getAllSurfSpots();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(surfSpotRepository).findAll();
    }

    @Test
    public void testFindSurfSpotsWithinBounds() {
        BoundingBox boundingBox = new BoundingBox(10.0, 20.0, 30.0, 40.0);

        Continent continent = new Continent();
        continent.setName("Africa");
        
        Country country = new Country();
        country.setName("Morocco");
        country.setContinent(continent);
        
        Region region = new Region();
        region.setName("Taghazout");
        region.setCountry(country);
        
        SurfSpot spot1 = new SurfSpot();
        spot1.setRegion(region);
        
        SurfSpot spot2 = new SurfSpot();
        spot2.setRegion(region);
        
        List<SurfSpot> mockSurfSpots = Arrays.asList(spot1, spot2);

        when(surfSpotRepository.findWithinBounds(10.0, 20.0, 30.0, 40.0, testUserId))
                .thenReturn(mockSurfSpots);

        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsWithinBounds(boundingBox, testUserId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(surfSpotRepository).findWithinBounds(10.0, 20.0, 30.0, 40.0, testUserId);
    }

    @Test
    public void testFindSurfSpotsByRegionSlug() {
        String regionSlug = "region-slug";
        Continent continent = new Continent();
        continent.setName("Europe");
        
        Country country = new Country();
        country.setName("Portugal");
        country.setContinent(continent);
        
        Region mockRegion = new Region();
        mockRegion.setSlug(regionSlug);
        mockRegion.setCountry(country);

        SurfSpot mockSpot = new SurfSpot();
        mockSpot.setRegion(mockRegion);
        List<SurfSpot> mockSurfSpots = Collections.singletonList(mockSpot);
        
        when(regionRepository.findBySlug(regionSlug)).thenReturn(Optional.of(mockRegion));
        when(surfSpotRepository.findByRegion(mockRegion, testUserId)).thenReturn(mockSurfSpots);

        List<SurfSpotDTO> result = surfSpotService.findSurfSpotsByRegionSlug(regionSlug, testUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(regionRepository).findBySlug(regionSlug);
        verify(surfSpotRepository).findByRegion(mockRegion, testUserId);
    }

    @Test
    public void testCreateSurfSpot() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Test Spot");
        request.setDescription("Great surf spot!");
        request.setUserId(testUserId);
        request.setRegionId(1L);

        Region mockRegion = new Region();
        mockRegion.setId(1L);
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));

        SurfSpot mockSurfSpot = new SurfSpot();
        mockSurfSpot.setName(request.getName());
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
    public void testUpdateSurfSpot() {
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
    public void testUpdateSurfSpotFailedSurfSpotNotFound() {
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
}
