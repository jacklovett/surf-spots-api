package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SurfSpotType;
import com.lovettj.surfspotsapi.requests.BoundingBox;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SurfSpotControllerTest {

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
  void testGetAllSurfSpots() throws Exception {
    Mockito.when(surfSpotService.getAllSurfSpots()).thenReturn(Collections.singletonList(new SurfSpot()));

    mockMvc.perform(get("/api/surf-spots")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void testGetSurfSpotsByRegion() throws Exception {
    Mockito.when(surfSpotService.findSurfSpotsByRegionSlug("oahu")).thenReturn(Collections.singletonList(surfSpotDTO));

    mockMvc.perform(get("/api/surf-spots/region/oahu")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name", is("Pipeline")));
  }

  @Test
  void testGetSurfSpotBySlug() throws Exception {
    Mockito.when(surfSpotService.findBySlugAndUserId("pipeline", null)).thenReturn(Optional.of(surfSpotDTO));

    mockMvc.perform(get("/api/surf-spots/pipeline")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Pipeline")));
  }

  @Test
  void testGetSurfSpotById() throws Exception {
    SurfSpot surfSpot = SurfSpot.builder()
        .id(1L)
        .name("Pipeline")
        .description("A famous surf spot.")
        .type(SurfSpotType.REEF_BREAK)
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();

    Mockito.when(surfSpotService.getSurfSpotById(1L)).thenReturn(Optional.of(surfSpot));

    mockMvc.perform(get("/api/surf-spots/id/1")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Pipeline")));
  }

  @Test
  void testGetSurfSpotsWithinBounds() throws Exception {
    Mockito.when(surfSpotService.findSurfSpotsWithinBounds(Mockito.any(BoundingBox.class), Mockito.isNull()))
        .thenReturn(Collections.singletonList(surfSpotDTO));

    mockMvc.perform(post("/api/surf-spots/within-bounds")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "minLat": 21.2,
              "minLng": -158.1,
              "maxLat": 21.7,
              "maxLng": -157.7
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name", is("Pipeline")));
  }

  @Test
  void testCreateSurfSpot() throws Exception {
    SurfSpot surfSpot = SurfSpot.builder()
        .id(1L)
        .name("Pipeline")
        .description("A famous surf spot.")
        .type(SurfSpotType.REEF_BREAK)
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();

    Mockito.when(surfSpotService.createSurfSpot(Mockito.any(SurfSpot.class))).thenReturn(surfSpot);

    mockMvc.perform(post("/api/surf-spots")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Pipeline",
              "description": "A famous surf spot.",
              "type": "Reef Break",
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Pipeline")));
  }

  @Test
  void testUpdateSurfSpot() throws Exception {
    SurfSpot updatedSurfSpot = SurfSpot.builder()
        .id(1L)
        .name("Updated Pipeline")
        .description("An updated famous surf spot.")
        .type(SurfSpotType.REEF_BREAK)
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();

    Mockito.when(surfSpotService.updateSurfSpot(Mockito.eq(1L), Mockito.any(SurfSpot.class)))
        .thenReturn(updatedSurfSpot);

    mockMvc.perform(put("/api/surf-spots/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Updated Pipeline",
              "description": "An updated famous surf spot.",
              "type": "Reef Break"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("Updated Pipeline")));
  }

  @Test
  void testDeleteSurfSpot() throws Exception {
    Mockito.doNothing().when(surfSpotService).deleteSurfSpot(1L);

    mockMvc.perform(delete("/api/surf-spots/1")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    Mockito.verify(surfSpotService, Mockito.times(1)).deleteSurfSpot(1L);
  }
}
