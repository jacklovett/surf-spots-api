package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.RegionAndCountryResult;
import com.lovettj.surfspotsapi.dto.RegionLookupRequest;
import com.lovettj.surfspotsapi.entity.*;
import com.lovettj.surfspotsapi.service.RegionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionControllerTests {

    @Mock
    private RegionService regionService;

    @InjectMocks
    private RegionController regionController;

    private Continent testContinent;
    private Country testCountry;
    private Region testRegion1;
    private Region testRegion2;

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
    }

    @Test
    void testGetRegionBySlugShouldReturnRegionWhenSlugExists() {
        // Arrange
        String slug = "taghazout";
        when(regionService.getRegionBySlug(slug)).thenReturn(testRegion1);

        // Act
        ResponseEntity<Region> response = regionController.getRegionBySlug(slug);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Region body = response.getBody();
        assertNotNull(body);
        assertEquals(testRegion1.getId(), body.getId());
        assertEquals(testRegion1.getName(), body.getName());
        verify(regionService).getRegionBySlug(slug);
    }

    @Test
    void testGetRegionsByCountryIdShouldReturnRegionsWhenCountryIdExists() {
        // Arrange
        Long countryId = 1L;
        List<Region> expectedRegions = Arrays.asList(testRegion1, testRegion2);
        when(regionService.getRegionsByCountry(countryId)).thenReturn(expectedRegions);

        // Act
        ResponseEntity<List<Region>> response = regionController.getRegionsByCountryId(countryId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Region> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals(testRegion1, body.get(0));
        assertEquals(testRegion2, body.get(1));
        verify(regionService).getRegionsByCountry(countryId);
    }

    @Test
    void testGetRegionsByCountrySlugShouldReturnRegionsWhenCountrySlugExists() {
        // Arrange
        String countrySlug = "morocco";
        List<Region> expectedRegions = Arrays.asList(testRegion1, testRegion2);
        when(regionService.findRegionsByCountrySlug(countrySlug)).thenReturn(expectedRegions);

        // Act
        ResponseEntity<List<Region>> response = regionController.getRegionsByCountrySlug(countrySlug);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Region> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals(testRegion1, body.get(0));
        assertEquals(testRegion2, body.get(1));
        verify(regionService).findRegionsByCountrySlug(countrySlug);
    }

    @Test
    void testGetRegionsByCountrySlugShouldReturnNotFoundWhenNoRegionsFound() {
        // Arrange
        String countrySlug = "morocco";
        when(regionService.findRegionsByCountrySlug(countrySlug)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<Region>> response = regionController.getRegionsByCountrySlug(countrySlug);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(regionService).findRegionsByCountrySlug(countrySlug);
    }

    @Test
    void testGetRegionAndCountryByCoordinatesShouldReturnBothWhenFound() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        String countryName = "morocco";
        RegionLookupRequest request = new RegionLookupRequest(longitude, latitude, countryName);
        RegionAndCountryResult expectedResult = 
            new RegionAndCountryResult(testRegion1, testCountry);
        when(regionService.findRegionAndCountryByCoordinates(longitude, latitude, countryName))
            .thenReturn(expectedResult);

        // Act
        ResponseEntity<RegionAndCountryResult> response = 
            regionController.getRegionAndCountryByCoordinates(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        RegionAndCountryResult body = response.getBody();
        assertNotNull(body);
        assertEquals(testRegion1, body.getRegion());
        assertEquals(testCountry, body.getCountry());
        verify(regionService).findRegionAndCountryByCoordinates(longitude, latitude, countryName);
    }

    @Test
    void testGetRegionAndCountryByCoordinatesShouldReturnCountryWhenRegionNotFound() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        String countryName = "morocco";
        RegionLookupRequest request = new RegionLookupRequest(longitude, latitude, countryName);
        // Region is null but country is found
        RegionAndCountryResult expectedResult = 
            new RegionAndCountryResult(null, testCountry);
        when(regionService.findRegionAndCountryByCoordinates(longitude, latitude, countryName))
            .thenReturn(expectedResult);

        // Act
        ResponseEntity<RegionAndCountryResult> response = 
            regionController.getRegionAndCountryByCoordinates(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        RegionAndCountryResult body = response.getBody();
        assertNotNull(body);
        assertNull(body.getRegion());
        assertEquals(testCountry, body.getCountry());
        verify(regionService).findRegionAndCountryByCoordinates(longitude, latitude, countryName);
    }

    @Test
    void testGetRegionAndCountryByCoordinatesShouldReturnNotFoundWhenCountryNotFound() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        String countryName = "nonexistent";
        RegionLookupRequest request = new RegionLookupRequest(longitude, latitude, countryName);
        when(regionService.findRegionAndCountryByCoordinates(longitude, latitude, countryName))
            .thenReturn(null);

        // Act
        ResponseEntity<RegionAndCountryResult> response = 
            regionController.getRegionAndCountryByCoordinates(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(regionService).findRegionAndCountryByCoordinates(longitude, latitude, countryName);
    }

    @Test
    void testGetRegionAndCountryByCoordinatesShouldHandleCaseInsensitiveCountryName() {
        // Arrange
        Double longitude = -9.7167;
        Double latitude = 30.5333;
        String countryName = "MOROCCO"; // Uppercase
        RegionLookupRequest request = new RegionLookupRequest(longitude, latitude, countryName);
        RegionAndCountryResult expectedResult = 
            new RegionAndCountryResult(testRegion1, testCountry);
        when(regionService.findRegionAndCountryByCoordinates(longitude, latitude, countryName))
            .thenReturn(expectedResult);

        // Act
        ResponseEntity<RegionAndCountryResult> response = 
            regionController.getRegionAndCountryByCoordinates(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        RegionAndCountryResult body = response.getBody();
        assertNotNull(body);
        assertEquals(testCountry, body.getCountry());
        verify(regionService).findRegionAndCountryByCoordinates(longitude, latitude, countryName);
    }
}

