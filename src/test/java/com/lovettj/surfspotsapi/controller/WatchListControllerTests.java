package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.WatchListDTO;
import com.lovettj.surfspotsapi.dto.WatchListSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.service.WatchListService;

@WebMvcTest(WatchListController.class)
class WatchListControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WatchListService watchListService;

    private User testUser;
    private SurfSpot testSpot;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-id-123";
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        testSpot = new SurfSpot();
        testSpot.setId(1L);
        testSpot.setName("Test Spot");
    }

    @Test
    void testAddWatchListSurfSpotShouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/watch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"" + testUserId + "\", \"surfSpotId\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void testAddWatchListSurfSpotShouldReturnOkWhenAuthenticated() throws Exception {
        doNothing().when(watchListService).addSurfSpotToWatchList(anyString(), anyLong());

        mockMvc.perform(post("/api/watch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"" + testUserId + "\", \"surfSpotId\": 1}"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUsersWatchListShouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/watch/" + testUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void testGetUsersWatchListShouldReturnOkWhenAuthenticated() throws Exception {
        List<WatchListSpotDTO> spots = new ArrayList<>();
        spots.add(WatchListSpotDTO.builder()
            .surfSpot(SurfSpotDTO.builder()
                .id(1L)
                .name("Test Spot")
                .build())
            .addedAt(null)
            .build());

        when(watchListService.getUsersWatchList(anyString())).thenReturn(WatchListDTO.builder()
            .surfSpots(spots)
            .notifications(new ArrayList<>())
            .build());

        mockMvc.perform(get("/api/watch/" + testUserId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testGetUsersWatchListShouldReturnOkWhenAuthenticatedWithEmptyLists() throws Exception {
        when(watchListService.getUsersWatchList(anyString())).thenReturn(WatchListDTO.builder()
            .surfSpots(new ArrayList<>())
            .notifications(new ArrayList<>())
            .build());

        mockMvc.perform(get("/api/watch/" + testUserId))
                .andExpect(status().isOk());
    }

    @Test
    void testRemoveWatchListSurfSpotShouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/watch/" + testUserId + "/remove/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void testRemoveWatchListSurfSpotShouldReturnOkWhenAuthenticated() throws Exception {
        doNothing().when(watchListService).removeSurfSpotFromWishList(anyString(), anyLong());

        mockMvc.perform(delete("/api/watch/" + testUserId + "/remove/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testRemoveWatchListSurfSpotWithInvalidIdsShouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/watch/" + testUserId + "/remove/abc"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void testRemoveWatchListSurfSpotWithInvalidIdsShouldReturnBadRequestWhenAuthenticated() throws Exception {
        String invalidUserId = "invalid-user-id";

        mockMvc.perform(delete("/api/watch/" + invalidUserId + "/remove/abc@#$"))
            .andExpect(status().isBadRequest());
    }
}
