package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SurfSpotType;
import com.lovettj.surfspotsapi.service.SurfSpotService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(SurfSpotController.class)
class SurfSpotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SurfSpotService surfSpotService;

    private Region region;

    @BeforeEach
    public void setUp() {
        region = new Region(1L, "Oahu", "", null, null);
    }

    @Test
    void testGetAllSurfSpots() throws Exception {
        SurfSpot spot = SurfSpot.builder()
                .id(1L)
                .name("Pipeline")
                .description("A famous surf spot.")
                .type(SurfSpotType.REEF_BREAK)
                .region(region)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        Mockito.when(surfSpotService.getAllSurfSpots()).thenReturn(Collections.singletonList(spot));

        mockMvc.perform(get("/api/surfspots")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Pipeline")))
                .andExpect(jsonPath("$[0].description", is("A famous surf spot.")))
                .andExpect(jsonPath("$[0].type", is("Reef Break")))
                .andExpect(jsonPath("$[0].region.name", is("Oahu")));
    }

    @Test
    void testGetSurfSpotById() throws Exception {
        SurfSpot spot = SurfSpot.builder()
                .id(1L)
                .name("Pipeline")
                .description("A famous surf spot.")
                .type(SurfSpotType.REEF_BREAK)
                .region(region)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        Mockito.when(surfSpotService.getSurfSpotById(1L)).thenReturn(Optional.of(spot));

        mockMvc.perform(get("/api/surfspots/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Pipeline")))
                .andExpect(jsonPath("$.description", is("A famous surf spot.")))
                .andExpect(jsonPath("$.type", is("Reef Break")))
                .andExpect(jsonPath("$.region.name", is("Oahu")));
    }

    @Test
    void testCreateSurfSpot() throws Exception {
        SurfSpot spot = SurfSpot.builder()
                .id(1L)
                .name("Pipeline")
                .description("A famous surf spot.")
                .type(SurfSpotType.REEF_BREAK)
                .region(region)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        Mockito.when(surfSpotService.createSurfSpot(Mockito.any(SurfSpot.class))).thenReturn(spot);

        mockMvc.perform(post("/api/surfspots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Pipeline",
                            "description": "A famous surf spot.",
                            "type": "Reef Break",
                            "region": {
                                "id": 1,
                                "name": "Oahu"
                            }
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Pipeline")))
                .andExpect(jsonPath("$.description", is("A famous surf spot.")))
                .andExpect(jsonPath("$.type", is("Reef Break")))
                .andExpect(jsonPath("$.region.name", is("Oahu")));
    }

    @Test
    void testUpdateSurfSpot() throws Exception {
        SurfSpot updatedSpot = SurfSpot.builder()
                .id(1L)
                .name("Updated Pipeline")
                .description("An updated famous surf spot.")
                .type(SurfSpotType.REEF_BREAK)
                .region(region)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        Mockito.when(surfSpotService.updateSurfSpot(Mockito.eq(1L), Mockito.any(SurfSpot.class)))
                .thenReturn(updatedSpot);

        mockMvc.perform(put("/api/surfspots/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Updated Pipeline",
                            "description": "An updated famous surf spot.",
                            "type": "Reef Break",
                            "region": {
                                "id": 1,
                                "name": "Oahu"
                            }
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Pipeline")))
                .andExpect(jsonPath("$.description", is("An updated famous surf spot.")))
                .andExpect(jsonPath("$.type", is("Reef Break")))
                .andExpect(jsonPath("$.region.name", is("Oahu")));
    }

    @Test
    void testDeleteSurfSpot() throws Exception {
        Mockito.doNothing().when(surfSpotService).deleteSurfSpot(1L);

        mockMvc.perform(delete("/api/surfspots/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
