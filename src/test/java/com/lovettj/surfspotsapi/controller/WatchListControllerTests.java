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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.WatchListDTO;
import com.lovettj.surfspotsapi.dto.WatchListSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.service.NotificationService;
import com.lovettj.surfspotsapi.service.SwellSeasonService;
import com.lovettj.surfspotsapi.service.WatchListService;

@SpringBootTest
@AutoConfigureMockMvc
class WatchListControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WatchListService watchListService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private SwellSeasonService swellSeasonService;

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

    private Cookie createValidSessionCookie() {
        // SessionCookieFilter expects a cookie with exactly two parts (payload.signature)
        // Format: "payload.signature" - when split by ".", should have exactly 2 parts
        Cookie sessionCookie = new Cookie("session", "testpayload.testsignature");
        return sessionCookie;
    }

    @Test
    void testAddWatchListSurfSpotShouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/watch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"" + testUserId + "\", \"surfSpotId\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddWatchListSurfSpotShouldReturnOkWhenAuthenticated() throws Exception {
        doNothing().when(watchListService).addSurfSpotToWatchList(anyString(), anyLong());

        mockMvc.perform(post("/api/watch")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(createValidSessionCookie())
                .content("{\"userId\": \"" + testUserId + "\", \"surfSpotId\": 1}"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUsersWatchListShouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/watch/" + testUserId))
                .andExpect(status().isForbidden());
    }

    @Test
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

        mockMvc.perform(get("/api/watch/" + testUserId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUsersWatchListShouldReturnOkWhenAuthenticatedWithEmptyLists() throws Exception {
        when(watchListService.getUsersWatchList(anyString())).thenReturn(WatchListDTO.builder()
            .surfSpots(new ArrayList<>())
            .notifications(new ArrayList<>())
            .build());

        mockMvc.perform(get("/api/watch/" + testUserId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void testRemoveWatchListSurfSpotShouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/watch/" + testUserId + "/remove/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRemoveWatchListSurfSpotShouldReturnOkWhenAuthenticated() throws Exception {
        doNothing().when(watchListService).removeSurfSpotFromWishList(anyString(), anyLong());

        mockMvc.perform(delete("/api/watch/" + testUserId + "/remove/1")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void testRemoveWatchListSurfSpotWithInvalidIdsShouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/watch/" + testUserId + "/remove/abc"))
            .andExpect(status().isForbidden());
    }

    @Test
    void testRemoveWatchListSurfSpotWithInvalidIdsShouldReturnBadRequestWhenAuthenticated() throws Exception {
        String invalidUserId = "invalid-user-id";

        mockMvc.perform(delete("/api/watch/" + invalidUserId + "/remove/abc@#$")
                .cookie(createValidSessionCookie()))
            .andExpect(status().isBadRequest());
    }
}
