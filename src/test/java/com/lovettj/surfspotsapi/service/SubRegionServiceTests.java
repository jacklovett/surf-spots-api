package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.repository.RegionRepository;
import com.lovettj.surfspotsapi.repository.SubRegionRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubRegionServiceTests {

    @Mock
    private SubRegionRepository subRegionRepository;

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private SubRegionService subRegionService;

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
    void testGetSubRegionBySlugShouldReturnSubRegionWhenSlugExists() {
        // Arrange
        String slug = "test-sub-region-1";
        when(subRegionRepository.findBySlug(slug)).thenReturn(Optional.of(testSubRegion1));

        // Act
        SubRegion result = subRegionService.getSubRegionBySlug(slug);

        // Assert
        assertNotNull(result);
        assertEquals(testSubRegion1.getId(), result.getId());
        assertEquals(testSubRegion1.getName(), result.getName());
        assertEquals(testSubRegion1.getSlug(), result.getSlug());
        verify(subRegionRepository).findBySlug(slug);
    }

    @Test
    void testGetSubRegionBySlugShouldThrowEntityNotFoundExceptionWhenSlugDoesNotExist() {
        // Arrange
        String slug = "non-existent-slug";
        when(subRegionRepository.findBySlug(slug)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> subRegionService.getSubRegionBySlug(slug));
        assertEquals("SubRegion not found", exception.getMessage());
        verify(subRegionRepository).findBySlug(slug);
    }

    @Test
    void testGetSubRegionsByRegionShouldReturnSubRegionsWhenRegionIdExists() {
        // Arrange
        Long regionId = 1L;
        List<SubRegion> expectedSubRegions = Arrays.asList(testSubRegion1, testSubRegion2);
        when(subRegionRepository.findByRegionId(regionId)).thenReturn(expectedSubRegions);

        // Act
        List<SubRegion> result = subRegionService.getSubRegionsByRegion(regionId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testSubRegion1, result.get(0));
        assertEquals(testSubRegion2, result.get(1));
        verify(subRegionRepository).findByRegionId(regionId);
    }

    @Test
    void testGetSubRegionsByRegionShouldReturnEmptyListWhenRegionIdDoesNotExist() {
        // Arrange
        Long regionId = 999L;
        when(subRegionRepository.findByRegionId(regionId)).thenReturn(Arrays.asList());

        // Act
        List<SubRegion> result = subRegionService.getSubRegionsByRegion(regionId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(subRegionRepository).findByRegionId(regionId);
    }

    @Test
    void testFindSubRegionsByRegionSlugShouldReturnSubRegionsWhenRegionSlugExists() {
        // Arrange
        String regionSlug = "test-region";
        List<SubRegion> expectedSubRegions = Arrays.asList(testSubRegion1, testSubRegion2);
        when(regionRepository.findBySlug(regionSlug)).thenReturn(Optional.of(testRegion));
        when(subRegionRepository.findByRegion(testRegion)).thenReturn(expectedSubRegions);

        // Act
        List<SubRegion> result = subRegionService.findSubRegionsByRegionSlug(regionSlug);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testSubRegion1, result.get(0));
        assertEquals(testSubRegion2, result.get(1));
        verify(regionRepository).findBySlug(regionSlug);
        verify(subRegionRepository).findByRegion(testRegion);
    }

    @Test
    void testFindSubRegionsByRegionSlugShouldThrowEntityNotFoundExceptionWhenRegionSlugDoesNotExist() {
        // Arrange
        String regionSlug = "non-existent-region";
        when(regionRepository.findBySlug(regionSlug)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> subRegionService.findSubRegionsByRegionSlug(regionSlug));
        assertEquals("Region not found", exception.getMessage());
        verify(regionRepository).findBySlug(regionSlug);
        verify(subRegionRepository, never()).findByRegion(any());
    }

    @Test
    void testFindSubRegionsByRegionSlugShouldReturnEmptyListWhenRegionHasNoSubRegions() {
        // Arrange
        String regionSlug = "test-region";
        when(regionRepository.findBySlug(regionSlug)).thenReturn(Optional.of(testRegion));
        when(subRegionRepository.findByRegion(testRegion)).thenReturn(Arrays.asList());

        // Act
        List<SubRegion> result = subRegionService.findSubRegionsByRegionSlug(regionSlug);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(regionRepository).findBySlug(regionSlug);
        verify(subRegionRepository).findByRegion(testRegion);
    }
}
