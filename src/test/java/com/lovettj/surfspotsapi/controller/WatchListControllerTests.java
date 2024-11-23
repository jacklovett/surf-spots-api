package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.service.WatchListService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WatchListControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WatchListService watchListService;

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetUsersWatchList() throws Exception {
        SurfSpot spot1 = new SurfSpot();
        spot1.setId(1L);
        spot1.setName("Pipeline");

        when(watchListService.getUsersWatchList(1L)).thenReturn(List.of(spot1));

        mockMvc.perform(get("/api/watch/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Pipeline"));

        verify(watchListService, times(1)).getUsersWatchList(1L);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testAddWatchListSurfSpot() throws Exception {
        doNothing().when(watchListService).addSurfSpotToWatchList(1L, 2L);

        mockMvc.perform(post("/api/watch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": 1,
                        "surfSpotId": 2
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(content().string("Surf spot added to user’s watch list."));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testRemoveWatchListSurfSpot() throws Exception {
        doNothing().when(watchListService).removeSurfSpotFromWishList(1L, 2L);

        mockMvc.perform(delete("/api/watch/1/remove/2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("Surf spot removed from user’s watch list."));
    }
}
