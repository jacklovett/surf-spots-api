package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.entity.*;
import com.lovettj.surfspotsapi.repository.CountryRepository;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionServiceTests {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private RegionService regionService;

    private Continent testContinent;
    private Country testCountry;
    private Region testRegion1;
    private Region testRegion2;
    private SurfSpot testSurfSpot1;
    private SurfSpot testSurfSpot2;

    @BeforeEach
    void setUp() {
        testContinent = Continent.builder()
                .id(1L)
                .name("Africa")
                .build();
        testContinent.generateSlug();

        testCountry = Country.builder()
                .id(1L)
                .name("Morocco")
                .continent(testContinent)
                .build();
        testCountry.generateSlug();

        testRegion1 = Region.builder()
                .id(1L)
                .name("Taghazout")
                .country(testCountry)
                .build();
        testRegion1.generateSlug();

        testRegion2 = Region.builder()
                .id(2L)
                .name("Essaouira")
                .country(testCountry)
                .build();
        testRegion2.generateSlug();

        testSurfSpot1 = SurfSpot.builder()
                .id(1L)
                .name("Anchor Point")
                .latitude(30.5333)
                .longitude(-9.7167)
                .region(testRegion1)
                .build();
        testSurfSpot1.generateSlug();

        testSurfSpot2 = SurfSpot.builder()
                .id(2L)
                .name("Killer Point")
                .latitude(30.5500)
                .longitude(-9.7000)
                .region(testRegion1)
                .build();
        testSurfSpot2.generateSlug();
    }

    @Test
    void testGetRegionBySlugShouldReturnRegionWhenSlugExists() {
        // Arrange
        String slug = "taghazout";
        when(regionRepository.findBySlug(slug)).thenReturn(Optional.of(testRegion1));

        // Act
        Region result = regionService.getRegionBySlug(slug);

        // Assert
        assertNotNull(result);
        assertEquals(testRegion1.getId(), result.getId());
        assertEquals(testRegion1.getName(), result.getName());
        verify(regionRepository).findBySlug(slug);
    }

    @Test
    void testGetRegionBySlugShouldThrowEntityNotFoundExceptionWhenSlugDoesNotExist() {
        // Arrange
        String slug = "non-existent-slug";
        when(regionRepository.findBySlug(slug)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> regionService.getRegionBySlug(slug));
        assertEquals("Region not found", exception.getMessage());
        verify(regionRepository).findBySlug(slug);
    }

    @Test
    void testGetRegionsByCountryShouldReturnRegionsWhenCountryIdExists() {
        // Arrange
        Long countryId = 1L;
        List<Region> expectedRegions = Arrays.asList(testRegion1, testRegion2);
        when(regionRepository.findByCountryId(countryId)).thenReturn(expectedRegions);

        // Act
        List<Region> result = regionService.getRegionsByCountry(countryId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testRegion1, result.get(0));
        assertEquals(testRegion2, result.get(1));
        verify(regionRepository).findByCountryId(countryId);
    }

    @Test
    void testFindRegionsByCountrySlugShouldReturnRegionsWhenCountrySlugExists() {
        // Arrange
        String countrySlug = "morocco";
        List<Region> expectedRegions = Arrays.asList(testRegion1, testRegion2);
        when(countryRepository.findBySlug(countrySlug)).thenReturn(Optional.of(testCountry));
        when(regionRepository.findByCountryId(testCountry.getId())).thenReturn(expectedRegions);

        // Act
        List<Region> result = regionService.findRegionsByCountrySlug(countrySlug);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testRegion1, result.get(0));
        assertEquals(testRegion2, result.get(1));
        verify(countryRepository).findBySlug(countrySlug);
        verify(regionRepository).findByCountryId(testCountry.getId());
    }

    @Test
    void testFindRegionsByCountrySlugShouldThrowEntityNotFoundExceptionWhenCountrySlugDoesNotExist() {
        // Arrange
        String countrySlug = "non-existent-country";
        when(countryRepository.findBySlug(countrySlug)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> regionService.findRegionsByCountrySlug(countrySlug));
        assertEquals("Country not found", exception.getMessage());
        verify(countryRepository).findBySlug(countrySlug);
        verify(regionRepository, never()).findByCountryId(any());
    }

    @Test
    void testFindRegionByCoordinatesShouldReturnRegionWithBoundingBoxWhenPointIsWithinBoundingBox() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        
        // Create a region with bounding box
        Double[] boundingBox = new Double[]{-10.0, 30.0, -9.0, 31.0}; // [minLong, minLat, maxLong, maxLat]
        testRegion1.setBoundingBox(boundingBox);
        
        when(regionRepository.findRegionContainingPoint(longitude, latitude, null))
            .thenReturn(Optional.of(testRegion1));

        // Act
        Region result = regionService.findRegionByCoordinates(longitude, latitude);

        // Assert
        assertNotNull(result);
        assertEquals(testRegion1.getId(), result.getId());
        verify(regionRepository).findRegionContainingPoint(longitude, latitude, null);
    }

    @Test
    void testFindRegionByCoordinatesShouldReturnClosestRegionWhenNoBoundingBoxAvailable() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        
        // Set up regions with surf spots but no bounding boxes
        testRegion1.setSurfSpots(Arrays.asList(testSurfSpot1, testSurfSpot2));
        testRegion2.setSurfSpots(Arrays.asList());
        
        when(regionRepository.findRegionContainingPoint(longitude, latitude, null))
            .thenReturn(Optional.empty());
        when(regionRepository.findRegionNearPoint(eq(longitude), eq(latitude), anyDouble(), eq(null)))
            .thenReturn(Optional.empty());
        when(regionRepository.findAllWithSurfSpots(null))
            .thenReturn(Arrays.asList(testRegion1, testRegion2));

        // Act
        Region result = regionService.findRegionByCoordinates(longitude, latitude);

        // Assert
        assertNotNull(result);
        assertEquals(testRegion1.getId(), result.getId());
        verify(regionRepository).findAllWithSurfSpots(null);
    }

    @Test
    void testFindRegionByCoordinatesShouldReturnNullWhenNoRegionFound() {
        // Arrange
        Double longitude = 0.0;
        Double latitude = 0.0;
        
        // Create regions with surf spots far away (more than 50km)
        SurfSpot farSurfSpot = SurfSpot.builder()
                .id(3L)
                .name("Far Spot")
                .latitude(50.0) // Very far from test coordinates
                .longitude(50.0)
                .region(testRegion1)
                .build();
        testRegion1.setSurfSpots(Arrays.asList(farSurfSpot));
        
        // Act
        Region result = regionService.findRegionByCoordinates(longitude, latitude);

        // Assert
        assertNull(result);
        verify(regionRepository).findAllWithSurfSpots(null);
    }

    @Test
    void testFindRegionByCoordinatesShouldPreferBoundingBoxOverClosestSurfSpot() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        
        // Region 1 has bounding box
        Double[] boundingBox1 = new Double[]{-10.0, 30.0, -9.0, 31.0};
        testRegion1.setBoundingBox(boundingBox1);
        
        // Region 2 has closer surf spot but no bounding box
        SurfSpot closerSurfSpot = SurfSpot.builder()
                .id(3L)
                .name("Closer Spot")
                .latitude(30.5334) // Very close to test coordinates
                .longitude(-9.7168)
                .region(testRegion2)
                .build();
        testRegion2.setSurfSpots(Arrays.asList(closerSurfSpot));
        
        when(regionRepository.findRegionContainingPoint(longitude, latitude, null))
            .thenReturn(Optional.of(testRegion1));

        // Act
        Region result = regionService.findRegionByCoordinates(longitude, latitude);

        // Assert
        assertNotNull(result);
        assertEquals(testRegion1.getId(), result.getId()); // Should prefer bounding box match
        verify(regionRepository).findRegionContainingPoint(longitude, latitude, null);
    }

    @Test
    void testFindRegionByCoordinatesShouldReturnNullWhenNoRegionsExist() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        when(regionRepository.findRegionContainingPoint(longitude, latitude, null))
            .thenReturn(Optional.empty());
        when(regionRepository.findRegionNearPoint(eq(longitude), eq(latitude), anyDouble(), eq(null)))
            .thenReturn(Optional.empty());
        when(regionRepository.findAllWithSurfSpots(null))
            .thenReturn(Arrays.asList());

        // Act
        Region result = regionService.findRegionByCoordinates(longitude, latitude);

        // Assert
        assertNull(result);
        verify(regionRepository).findAllWithSurfSpots(null);
    }

    @Test
    void testFindRegionByCoordinatesShouldHandleNullBoundingBox() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        
        testRegion1.setBoundingBox(null);
        testRegion1.setSurfSpots(Arrays.asList(testSurfSpot1));
        
        when(regionRepository.findRegionContainingPoint(longitude, latitude, null))
            .thenReturn(Optional.empty());
        when(regionRepository.findRegionNearPoint(eq(longitude), eq(latitude), anyDouble(), eq(null)))
            .thenReturn(Optional.empty());
        when(regionRepository.findAllWithSurfSpots(null))
            .thenReturn(Arrays.asList(testRegion1));

        // Act
        Region result = regionService.findRegionByCoordinates(longitude, latitude);

        // Assert
        assertNotNull(result);
        assertEquals(testRegion1.getId(), result.getId());
        verify(regionRepository).findAllWithSurfSpots(null);
    }

    @Test
    void testFindRegionByCoordinatesShouldHandleInvalidBoundingBoxLength() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        
        // Invalid bounding box (not 4 elements)
        Double[] invalidBoundingBox = new Double[]{-10.0, 30.0};
        testRegion1.setBoundingBox(invalidBoundingBox);
        testRegion1.setSurfSpots(Arrays.asList(testSurfSpot1));
        
        when(regionRepository.findRegionContainingPoint(longitude, latitude, null))
            .thenReturn(Optional.empty());
        when(regionRepository.findRegionNearPoint(eq(longitude), eq(latitude), anyDouble(), eq(null)))
            .thenReturn(Optional.empty());
        when(regionRepository.findAllWithSurfSpots(null))
            .thenReturn(Arrays.asList(testRegion1));

        // Act
        Region result = regionService.findRegionByCoordinates(longitude, latitude);

        // Assert
        assertNotNull(result);
        assertEquals(testRegion1.getId(), result.getId()); // Should fall back to closest surf spot
        verify(regionRepository).findAllWithSurfSpots(null);
    }
}

