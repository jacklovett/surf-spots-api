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
import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import com.lovettj.surfspotsapi.entity.SluggableEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
        region.setId(1L);
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
    void createSurfSpotShouldDetermineSwellSeasonForRiverWave() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("River Spot");
        request.setDescription("Standing river wave");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setRiverWave(true);

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));

        SwellSeason mockSwellSeason = new SwellSeason();
        mockSwellSeason.setId(1L);
        mockSwellSeason.setName("North Atlantic");
        mockSwellSeason.setStartMonth("September");
        mockSwellSeason.setEndMonth("April");
        when(swellSeasonDeterminationService.determineSwellSeason(36.5270, -6.2886))
                .thenReturn(Optional.of(mockSwellSeason));
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertNotNull(result.getSwellSeason());
        assertEquals("North Atlantic", result.getSwellSeason().getName());
        verify(swellSeasonDeterminationService).determineSwellSeason(36.5270, -6.2886);
    }

    @Test
    void createSurfSpotShouldNotCallSwellSeasonDeterminationForWavepool() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Wave Pool");
        request.setDescription("Indoor pool");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setWavepool(true);
        request.setStatus(SurfSpotStatus.PRIVATE);

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertNull(result.getSwellSeason());
        verify(swellSeasonDeterminationService, never()).determineSwellSeason(any(Double.class), any(Double.class));
    }

    @Test
    void createSurfSpotShouldPersistNoForecastsWhenWavepool() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Wave Pool");
        request.setDescription("Indoor pool");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setWavepool(true);
        request.setWavepoolUrl("https://wavepool.example.com/");
        request.setStatus(SurfSpotStatus.PRIVATE);
        request.setForecasts(Arrays.asList("https://forecast.example.com/should-not-persist"));

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertNotNull(result.getForecasts());
        assertTrue(result.getForecasts().isEmpty());
    }

    @Test
    void createSurfSpotShouldPersistNoWebcamsWhenWavepool() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Wave Pool");
        request.setDescription("Indoor pool");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setWavepool(true);
        request.setWavepoolUrl("https://wavepool.example.com/");
        request.setStatus(SurfSpotStatus.PRIVATE);
        request.setWebcams(Arrays.asList("https://webcam.example.com/should-not-persist"));

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertNotNull(result.getWebcams());
        assertTrue(result.getWebcams().isEmpty());
    }

    @Test
    void testCreateSurfSpotShouldSetForecastsAndWebcamsFromRequest() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Test Spot");
        request.setDescription("Great surf spot!");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setForecasts(Arrays.asList("https://forecast.example.com/spot1"));
        request.setWebcams(Arrays.asList("https://webcam.example.com/spot1"));

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));
        when(swellSeasonDeterminationService.determineSwellSeason(36.5270, -6.2886))
                .thenReturn(Optional.empty());
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertNotNull(result);
        assertEquals(request.getForecasts(), result.getForecasts());
        assertEquals(request.getWebcams(), result.getWebcams());
        verify(surfSpotRepository).save(any(SurfSpot.class));
    }

    @Test
    void testUpdateSurfSpotShouldSetForecastsAndWebcamsFromRequest() {
        SurfSpot existingSpot = new SurfSpot();
        existingSpot.setId(1L);
        existingSpot.setName("Original Name");
        existingSpot.setCreatedBy("test-user-id");
        existingSpot.setLatitude(36.5270);
        existingSpot.setLongitude(-6.2886);
        existingSpot.setForecasts(Collections.emptyList());
        existingSpot.setWebcams(Collections.emptyList());

        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Updated Name");
        request.setUserId("test-user-id");
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setRegionId(1L);
        request.setForecasts(Arrays.asList("https://forecast.example.com/updated"));
        request.setWebcams(Arrays.asList("https://webcam.example.com/updated"));

        when(swellSeasonDeterminationService.determineSwellSeason(36.5270, -6.2886))
                .thenReturn(Optional.empty());
        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));
        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SurfSpot result = surfSpotService.updateSurfSpot(1L, request);

        assertNotNull(result);
        assertEquals(request.getForecasts(), result.getForecasts());
        assertEquals(request.getWebcams(), result.getWebcams());
        verify(surfSpotRepository).save(any(SurfSpot.class));
    }

    @Test
    void updateSurfSpotShouldClearForecastsWhenWavepool() {
        SurfSpot existingSpot = new SurfSpot();
        existingSpot.setId(1L);
        existingSpot.setName("Ocean Spot");
        existingSpot.setCreatedBy("test-user-id");
        existingSpot.setLatitude(36.5270);
        existingSpot.setLongitude(-6.2886);
        existingSpot.setForecasts(Arrays.asList("https://forecast.example.com/old"));
        existingSpot.setWebcams(Arrays.asList("https://webcam.example.com/old"));

        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Now A Wavepool");
        request.setUserId("test-user-id");
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setRegionId(1L);
        request.setWavepool(true);
        request.setWavepoolUrl("https://wavepool.example.com/");
        request.setForecasts(Arrays.asList("https://forecast.example.com/ignored"));

        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));
        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SurfSpot result = surfSpotService.updateSurfSpot(1L, request);

        assertTrue(result.getForecasts().isEmpty());
        assertTrue(result.getWebcams().isEmpty());
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
    void testCreateSurfSpotShouldThrowConflictWhenSlugAlreadyExistsInRegion() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Test Spot");
        request.setDescription("Great surf spot!");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));

        String expectedSlug = SluggableEntity.slugFromName(request.getName());
        when(surfSpotRepository.existsByRegionIdAndSlug(1L, expectedSlug)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> surfSpotService.createSurfSpot(request)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testUpdateSurfSpotShouldThrowConflictWhenSlugAlreadyExistsInRegionExcludingSelf() {
        SurfSpot existingSpot = new SurfSpot();
        existingSpot.setId(1L);
        existingSpot.setName("Original Name");
        existingSpot.setCreatedBy(testUserId);
        existingSpot.setLatitude(36.5270);
        existingSpot.setLongitude(-6.2886);

        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Test Spot");
        request.setDescription("Updated Description");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);

        Region mockRegion = createMockRegion();
        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));

        String expectedSlug = SluggableEntity.slugFromName(request.getName());
        when(surfSpotRepository.existsByRegionIdAndSlugAndIdNot(1L, expectedSlug, 1L))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> surfSpotService.updateSurfSpot(1L, request)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    public void testCreateSurfSpotFailedUserNotFound() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setUserId(null); // Missing user ID

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> surfSpotService.createSurfSpot(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testUpdateSurfSpotShouldReturnUpdatedSurfSpot() {
        SurfSpot existingSpot = new SurfSpot();
        existingSpot.setId(1L);
        existingSpot.setName("Original Name");
        existingSpot.setCreatedBy("test-user-id");
        existingSpot.setLatitude(36.5270);
        existingSpot.setLongitude(-6.2886);

        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");
        request.setUserId("test-user-id");
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setRegionId(1L);

        SwellSeason mockSwellSeason = new SwellSeason();
        mockSwellSeason.setId(1L);
        mockSwellSeason.setName("North Atlantic");
        when(swellSeasonDeterminationService.determineSwellSeason(36.5270, -6.2886))
                .thenReturn(Optional.of(mockSwellSeason));

        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));
        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));
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
        existingSpot.setCreatedBy("test-user-id");
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
        request.setRegionId(1L);

        SwellSeason newSwellSeason = new SwellSeason();
        newSwellSeason.setId(2L);
        newSwellSeason.setName("Mediterranean");
        when(swellSeasonDeterminationService.determineSwellSeason(36.7213, -4.4214))
                .thenReturn(Optional.of(newSwellSeason));

        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));
        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));
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
        SurfSpot spot = createMockSurfSpot();
        spot.setCreatedBy(testUserId);
        when(surfSpotRepository.findById(surfSpotId)).thenReturn(Optional.of(spot));
        doNothing().when(surfSpotRepository).deleteById(surfSpotId);

        surfSpotService.deleteSurfSpot(surfSpotId, testUserId);

        verify(surfSpotRepository).findById(surfSpotId);
        verify(surfSpotRepository).deleteById(surfSpotId);
    }

    @Test
    public void testDeleteSurfSpotShouldThrowForbiddenWhenUserIdDoesNotMatchCreatedBy() {
        Long surfSpotId = 1L;
        SurfSpot spot = createMockSurfSpot();
        spot.setCreatedBy("owner-user-id");
        when(surfSpotRepository.findById(surfSpotId)).thenReturn(Optional.of(spot));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSpotService.deleteSurfSpot(surfSpotId, "different-user-id"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(surfSpotRepository).findById(surfSpotId);
        verify(surfSpotRepository, never()).deleteById(any());
    }

    @Test
    public void testUpdateSurfSpotShouldThrowForbiddenWhenUserIdDoesNotMatchCreatedBy() {
        SurfSpot existingSpot = createMockSurfSpot();
        existingSpot.setCreatedBy("owner-user-id");
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Updated Name");
        request.setUserId("different-user-id");
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSpotService.updateSurfSpot(1L, request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(surfSpotRepository).findById(1L);
        verify(surfSpotRepository, never()).save(any());
    }

    @Test
    public void testCreateSurfSpotShouldThrowBadRequestWhenForecastUrlIsInvalid() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Test Spot");
        request.setDescription("Great surf spot!");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setForecasts(Arrays.asList("javascript:alert(1)"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSpotService.createSurfSpot(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(surfSpotRepository, never()).save(any());
    }

    @Test
    public void testCreateSurfSpotShouldThrowBadRequestWhenWebcamUrlIsInvalid() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Test Spot");
        request.setDescription("Great surf spot!");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setWebcams(Arrays.asList("data:text/html,<script>alert(1)</script>"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> surfSpotService.createSurfSpot(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(surfSpotRepository, never()).save(any());
    }

    @Test
    void testSurfSpotDTOShouldIncludeForecastsAndWebcamsFromEntity() {
        SurfSpot surfSpot = createMockSurfSpot();
        surfSpot.setForecasts(Arrays.asList("https://forecast.example.com/1"));
        surfSpot.setWebcams(Arrays.asList("https://webcam.example.com/1"));

        SurfSpotDTO dto = new SurfSpotDTO(surfSpot);

        assertNotNull(dto.getForecasts());
        assertEquals(1, dto.getForecasts().size());
        assertEquals("https://forecast.example.com/1", dto.getForecasts().get(0));
        assertNotNull(dto.getWebcams());
        assertEquals(1, dto.getWebcams().size());
        assertEquals("https://webcam.example.com/1", dto.getWebcams().get(0));
    }

    @Test
    void testSurfSpotDTOShouldMapCrowdLevelFromEntity() {
        SurfSpot surfSpot = createMockSurfSpot();
        surfSpot.setCrowdLevel(CrowdLevel.FEW);

        SurfSpotDTO dto = new SurfSpotDTO(surfSpot);

        assertEquals(CrowdLevel.FEW, dto.getCrowdLevel());
    }

    @Test
    void testCreateSurfSpotShouldSetCrowdLevelFromRequest() {
        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Test Spot");
        request.setDescription("Great surf spot!");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setCrowdLevel(CrowdLevel.BUSY);

        Region mockRegion = createMockRegion();
        when(regionRepository.findById(1L)).thenReturn(Optional.of(mockRegion));
        when(swellSeasonDeterminationService.determineSwellSeason(36.5270, -6.2886))
                .thenReturn(Optional.empty());
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> {
            SurfSpot spot = invocation.getArgument(0);
            spot.setId(1L);
            return spot;
        });

        SurfSpot result = surfSpotService.createSurfSpot(request);

        assertEquals(CrowdLevel.BUSY, result.getCrowdLevel());
    }

    @Test
    void testUpdateSurfSpotShouldSetCrowdLevelFromRequest() {
        SurfSpot existingSpot = new SurfSpot();
        existingSpot.setId(1L);
        existingSpot.setName("Original Name");
        existingSpot.setCreatedBy(testUserId);
        existingSpot.setLatitude(36.5270);
        existingSpot.setLongitude(-6.2886);
        existingSpot.setCrowdLevel(CrowdLevel.EMPTY);

        SurfSpotRequest request = new SurfSpotRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");
        request.setUserId(testUserId);
        request.setRegionId(1L);
        request.setLatitude(36.5270);
        request.setLongitude(-6.2886);
        request.setCrowdLevel(CrowdLevel.PACKED);

        when(swellSeasonDeterminationService.determineSwellSeason(36.5270, -6.2886))
                .thenReturn(Optional.empty());
        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));
        when(regionRepository.findById(1L)).thenReturn(Optional.of(createMockRegion()));
        when(surfSpotRepository.save(any(SurfSpot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SurfSpot result = surfSpotService.updateSurfSpot(1L, request);

        assertEquals(CrowdLevel.PACKED, result.getCrowdLevel());
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
