package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.service.SubRegionService;
import jakarta.persistence.EntityNotFoundException;
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
class SubRegionControllerTests {

    @Mock
    private SubRegionService subRegionService;

    @InjectMocks
    private SubRegionController subRegionController;

    private Region testRegion;
    private SubRegion testSubRegion1;
    private SubRegion testSubRegion2;

    @BeforeEach
    void setUp() {
        testRegion = Region.builder()
                .id(1L)
                .name("Test Region")
                .description("Test region description")
                .build();
        testRegion.generateSlug();

        testSubRegion1 = SubRegion.builder()
                .id(1L)
                .name("Test Sub-Region 1")
                .description("Test sub-region 1 description")
                .region(testRegion)
                .build();
        testSubRegion1.generateSlug();

        testSubRegion2 = SubRegion.builder()
                .id(2L)
                .name("Test Sub-Region 2")
                .description("Test sub-region 2 description")
                .region(testRegion)
                .build();
        testSubRegion2.generateSlug();
    }

    @Test
    void testShouldReturnSubRegionWhenSlugExists() {
        // Arrange
        String slug = "test-sub-region-1";
        when(subRegionService.getSubRegionBySlug(slug)).thenReturn(testSubRegion1);

        // Act
        ResponseEntity<SubRegion> response = subRegionController.getSubRegionBySlug(slug);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SubRegion body = response.getBody();
        assertNotNull(body);
        assertEquals(testSubRegion1.getId(), body.getId());
        assertEquals(testSubRegion1.getName(), body.getName());
        verify(subRegionService).getSubRegionBySlug(slug);
    }

    @Test
    void testShouldThrowExceptionWhenSlugDoesNotExist() {
        // Arrange
        String slug = "non-existent-slug";
        when(subRegionService.getSubRegionBySlug(slug))
                .thenThrow(new EntityNotFoundException("SubRegion not found"));

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> subRegionController.getSubRegionBySlug(slug));
        assertEquals("SubRegion not found", exception.getMessage());
        verify(subRegionService).getSubRegionBySlug(slug);
    }

    @Test
    void testShouldReturnSubRegionsWhenRegionIdExists() {
        // Arrange
        Long regionId = 1L;
        List<SubRegion> expectedSubRegions = Arrays.asList(testSubRegion1, testSubRegion2);
        when(subRegionService.getSubRegionsByRegion(regionId)).thenReturn(expectedSubRegions);

        // Act
        ResponseEntity<List<SubRegion>> response = subRegionController.getSubRegionsByRegionId(regionId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<SubRegion> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals(testSubRegion1, body.get(0));
        assertEquals(testSubRegion2, body.get(1));
        verify(subRegionService).getSubRegionsByRegion(regionId);
    }

    @Test
    void testShouldReturnEmptyListWhenRegionIdDoesNotExist() {
        // Arrange
        Long regionId = 999L;
        when(subRegionService.getSubRegionsByRegion(regionId)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<SubRegion>> response = subRegionController.getSubRegionsByRegionId(regionId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<SubRegion> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isEmpty());
        verify(subRegionService).getSubRegionsByRegion(regionId);
    }

    @Test
    void testShouldReturnSubRegionsWhenRegionSlugExists() {
        // Arrange
        String regionSlug = "test-region";
        List<SubRegion> expectedSubRegions = Arrays.asList(testSubRegion1, testSubRegion2);
        when(subRegionService.findSubRegionsByRegionSlug(regionSlug)).thenReturn(expectedSubRegions);

        // Act
        ResponseEntity<List<SubRegion>> response = subRegionController.getSubRegionsByRegionSlug(regionSlug);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<SubRegion> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertEquals(testSubRegion1, body.get(0));
        assertEquals(testSubRegion2, body.get(1));
        verify(subRegionService).findSubRegionsByRegionSlug(regionSlug);
    }

    @Test
    void testShouldReturnNotFoundWhenRegionSlugDoesNotExist() {
        // Arrange
        String regionSlug = "non-existent-region";
        when(subRegionService.findSubRegionsByRegionSlug(regionSlug))
                .thenThrow(new EntityNotFoundException("Region not found"));

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> subRegionController.getSubRegionsByRegionSlug(regionSlug));
        assertEquals("Region not found", exception.getMessage());
        verify(subRegionService).findSubRegionsByRegionSlug(regionSlug);
    }

    @Test
    void testShouldReturnNotFoundWhenRegionHasNoSubRegions() {
        // Arrange
        String regionSlug = "test-region";
        when(subRegionService.findSubRegionsByRegionSlug(regionSlug)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<SubRegion>> response = subRegionController.getSubRegionsByRegionSlug(regionSlug);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(subRegionService).findSubRegionsByRegionSlug(regionSlug);
    }
}










