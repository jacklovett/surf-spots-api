package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.enums.SurfSpotType;
import com.lovettj.surfspotsapi.requests.BoundingBox;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.Arrays;

import jakarta.persistence.EntityNotFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SurfSpotControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SurfSpotService surfSpotService;

    private SurfSpotDTO surfSpotDTO;

    @BeforeEach
    public void setUp() {
        surfSpotDTO = SurfSpotDTO.builder()
                .id(1L)
                .name("Pipeline")
                .description("A famous surf spot.")
                .type(SurfSpotType.REEF_BREAK)
                .build();
    }

    @Test
    void testGetSurfSpotsByRegionWithFiltersShouldReturnFilteredSpots() throws Exception {
        SurfSpotDTO surfSpotDTO = SurfSpotDTO.builder().id(1L).name("Pipeline").build();
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setType(Arrays.asList(SurfSpotType.REEF_BREAK)); // Add more filter fields as needed
        String jsonBody = new ObjectMapper().writeValueAsString(filters);
        Mockito.when(surfSpotService.findSurfSpotsByRegionSlugWithFilters(Mockito.eq("oahu"), Mockito.any(SurfSpotFilterDTO.class)))
                .thenReturn(Collections.singletonList(surfSpotDTO));
        mockMvc.perform(post("/api/surf-spots/region/oahu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Pipeline")));
    }

    @Test
    void testGetSurfSpotBySlugShouldReturnSurfSpot() throws Exception {
        Mockito.when(surfSpotService.findBySlugAndUserId("pipeline", "test-user-id-123")).thenReturn(Optional.of(surfSpotDTO));

        mockMvc.perform(get("/api/surf-spots/pipeline")
                .contentType(MediaType.APPLICATION_JSON)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Pipeline")));
    }

    @Test
    void testGetSurfSpotByIdShouldReturnSurfSpotDTO() throws Exception {
        Mockito.when(surfSpotService.findByIdAndUserId(1L, "test-user-id-123"))
                .thenReturn(Optional.of(surfSpotDTO));

        mockMvc.perform(get("/api/surf-spots/id/1")
                .contentType(MediaType.APPLICATION_JSON)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Pipeline")));
    }

    @Test
    void testGetSurfSpotByIdShouldReturnSurfSpotDTOWithoutUserId() throws Exception {
        Mockito.when(surfSpotService.findByIdAndUserId(1L, null))
                .thenReturn(Optional.of(surfSpotDTO));

        mockMvc.perform(get("/api/surf-spots/id/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Pipeline")));
    }

    @Test
    void testGetSurfSpotByIdShouldReturnNotFoundWhenSurfSpotDoesNotExist() throws Exception {
        Mockito.when(surfSpotService.findByIdAndUserId(999L, "test-user-id-123"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/surf-spots/id/999")
                .contentType(MediaType.APPLICATION_JSON)
                .param("userId", "test-user-id-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetSurfSpotsWithinBoundsWithFiltersShouldReturnFilteredSpots() throws Exception {
        SurfSpotDTO surfSpotDTO = SurfSpotDTO.builder().id(1L).name("Pipeline").build();
        SurfSpotBoundsFilterDTO filters = new SurfSpotBoundsFilterDTO();
        filters.setMinLatitude(21.2);
        filters.setMaxLatitude(21.7);
        filters.setMinLongitude(-158.1);
        filters.setMaxLongitude(-157.7);

        filters.setType(Arrays.asList(SurfSpotType.BEACH_BREAK));
        String jsonBody = new ObjectMapper().writeValueAsString(filters);
        
        Mockito.when(surfSpotService.findSurfSpotsWithinBoundsWithFilters(Mockito.any(BoundingBox.class), Mockito.any(SurfSpotBoundsFilterDTO.class)))
                .thenReturn(Collections.singletonList(surfSpotDTO));
        mockMvc.perform(post("/api/surf-spots/within-bounds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Pipeline")));
    }

    @Test
    void testGetSurfSpotsBySubRegionWithFiltersShouldReturnFilteredSpots() throws Exception {
        // Arrange
        String subRegionSlug = "test-sub-region";
        SurfSpotDTO surfSpotDTO1 = SurfSpotDTO.builder()
                .id(1L)
                .name("Test Surf Spot 1")
                .path("/surf-spots/north-america/united-states/california/test-sub-region/test-surf-spot-1")
                .build();
        
        SurfSpotDTO surfSpotDTO2 = SurfSpotDTO.builder()
                .id(2L)
                .name("Test Surf Spot 2")
                .path("/surf-spots/north-america/united-states/california/test-sub-region/test-surf-spot-2")
                .build();
        
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setUserId("user123");
        String jsonBody = new ObjectMapper().writeValueAsString(filters);
        
        Mockito.when(surfSpotService.findSurfSpotsBySubRegionSlugWithFilters(Mockito.eq(subRegionSlug), Mockito.any(SurfSpotFilterDTO.class)))
                .thenReturn(Arrays.asList(surfSpotDTO1, surfSpotDTO2));

        // Act & Assert
        mockMvc.perform(post("/api/surf-spots/sub-region/" + subRegionSlug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Test Surf Spot 1")))
                .andExpect(jsonPath("$[1].name", is("Test Surf Spot 2")));
    }

    @Test
    void testGetSurfSpotsBySubRegionWithFiltersShouldReturnEmptyListWhenSubRegionHasNoSurfSpots() throws Exception {
        // Arrange
        String subRegionSlug = "test-sub-region";
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setUserId("user123");
        String jsonBody = new ObjectMapper().writeValueAsString(filters);
        
        Mockito.when(surfSpotService.findSurfSpotsBySubRegionSlugWithFilters(Mockito.eq(subRegionSlug), Mockito.any(SurfSpotFilterDTO.class)))
                .thenReturn(Arrays.asList());

        // Act & Assert - Should return 200 OK with empty list, not 404
        mockMvc.perform(post("/api/surf-spots/sub-region/" + subRegionSlug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetSurfSpotsBySubRegionWithFiltersShouldThrowExceptionWhenSubRegionDoesNotExist() throws Exception {
        // Arrange
        String subRegionSlug = "non-existent-sub-region";
        SurfSpotFilterDTO filters = new SurfSpotFilterDTO();
        filters.setUserId("user123");
        String jsonBody = new ObjectMapper().writeValueAsString(filters);
        
        Mockito.when(surfSpotService.findSurfSpotsBySubRegionSlugWithFilters(Mockito.eq(subRegionSlug), Mockito.any(SurfSpotFilterDTO.class)))
                .thenThrow(new EntityNotFoundException("SubRegion not found"));

        // Act & Assert
        mockMvc.perform(post("/api/surf-spots/sub-region/" + subRegionSlug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
                .andExpect(status().isInternalServerError());
    }
}
