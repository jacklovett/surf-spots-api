package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import com.lovettj.surfspotsapi.dto.UserSurfSpotsDTO;
import com.lovettj.surfspotsapi.service.UserSurfSpotService;
import com.lovettj.surfspotsapi.testutil.MockMvcDefaults;
import com.lovettj.surfspotsapi.testutil.SessionTestCookieFactory;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcDefaults.class)
class UserSurfSpotControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserSurfSpotService userSurfSpotService;

    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
    }

    private Cookie createValidSessionCookie() {
        return SessionTestCookieFactory.createSignedSessionCookie(testUserId);
    }

    @Test
    void testGetUserSurfSpotsSummaryShouldReturnOkWhenAuthenticated() throws Exception {
        UserSurfSpotsDTO dto = UserSurfSpotsDTO.builder()
                .totalCount(5)
                .countryCount(3)
                .continentCount(2)
                .surfedSpots(new ArrayList<>())
                .build();

        when(userSurfSpotService.getUserSurfSpotsSummary(testUserId)).thenReturn(dto);

        mockMvc.perform(get("/api/user-spots")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.countryCount").value(3))
                .andExpect(jsonPath("$.continentCount").value(2));

        verify(userSurfSpotService).getUserSurfSpotsSummary(testUserId);
    }

    @Test
    void testGetUserSurfSpotsSummaryShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user-spots"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserSurfSpotsSummaryShouldReturnEmptySummaryWhenUserHasNoSpots() throws Exception {
        UserSurfSpotsDTO dto = UserSurfSpotsDTO.builder()
                .totalCount(0)
                .countryCount(0)
                .continentCount(0)
                .surfedSpots(List.of())
                .build();

        when(userSurfSpotService.getUserSurfSpotsSummary(testUserId)).thenReturn(dto);

        mockMvc.perform(get("/api/user-spots")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void testAddUserSurfSpotShouldReturnOkWhenAuthenticated() throws Exception {
        doNothing().when(userSurfSpotService).addUserSurfSpot(anyString(), anyLong());

        mockMvc.perform(post("/api/user-spots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"surfSpotId\": 1}")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Surf spot added to user's list."));

        verify(userSurfSpotService).addUserSurfSpot(testUserId, 1L);
    }

    @Test
    void testAddUserSurfSpotShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/user-spots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"surfSpotId\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddUserSurfSpotShouldReturn500WhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("DB error")).when(userSurfSpotService).addUserSurfSpot(anyString(), anyLong());

        mockMvc.perform(post("/api/user-spots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"surfSpotId\": 1}")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRemoveUserSurfSpotShouldReturnOkWhenAuthenticated() throws Exception {
        doNothing().when(userSurfSpotService).removeUserSurfSpot(anyString(), anyLong());

        mockMvc.perform(delete("/api/user-spots/remove/1")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Surf spot removed from user's list."));

        verify(userSurfSpotService).removeUserSurfSpot(testUserId, 1L);
    }

    @Test
    void testRemoveUserSurfSpotShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/user-spots/remove/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRemoveUserSurfSpotShouldReturnBadRequestWhenSpotIdIsNotNumeric() throws Exception {
        mockMvc.perform(delete("/api/user-spots/remove/abc")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testToggleFavouriteShouldReturnOkWhenAuthenticated() throws Exception {
        doNothing().when(userSurfSpotService).toggleIsFavourite(anyString(), anyLong());

        mockMvc.perform(post("/api/user-spots/toggle-favourite/1")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk());

        verify(userSurfSpotService).toggleIsFavourite(testUserId, 1L);
    }

    @Test
    void testToggleFavouriteShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/user-spots/toggle-favourite/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testToggleFavouriteShouldReturnBadRequestWhenSpotIdIsNotNumeric() throws Exception {
        mockMvc.perform(post("/api/user-spots/toggle-favourite/invalid")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isBadRequest());
    }
}
