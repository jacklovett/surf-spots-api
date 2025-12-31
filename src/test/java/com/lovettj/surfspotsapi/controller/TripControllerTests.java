package com.lovettj.surfspotsapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lovettj.surfspotsapi.dto.TripDTO;
import com.lovettj.surfspotsapi.requests.*;
import com.lovettj.surfspotsapi.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TripControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    private String testUserId;
    private String testTripId;
    private TripDTO testTripDTO;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        testTripId = UUID.randomUUID().toString();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testTripDTO = TripDTO.builder()
                .id(testTripId)
                .ownerId(testUserId)
                .ownerName("Test User")
                .title("Test Trip")
                .description("Test Description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .isOwner(true)
                .spots(new ArrayList<>())
                .members(new ArrayList<>())
                .media(new ArrayList<>())
                .build();
    }

    private Cookie createValidSessionCookie() {
        return new Cookie("session", "testpayload.testsignature");
    }

    @Test
    void testCreateTripShouldReturnCreated() throws Exception {
        CreateTripRequest request = new CreateTripRequest();
        request.setTitle("New Trip");
        request.setDescription("Trip Description");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(5));

        when(tripService.createTrip(anyString(), any(CreateTripRequest.class))).thenReturn(testTripDTO);

        mockMvc.perform(post("/api/trips")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(testTripId))
                .andExpect(jsonPath("$.data.title").value("Test Trip"));
    }

    @Test
    void testCreateTripShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        CreateTripRequest request = new CreateTripRequest();
        request.setTitle("New Trip");

        mockMvc.perform(post("/api/trips")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateTripShouldReturnOk() throws Exception {
        UpdateTripRequest request = new UpdateTripRequest();
        request.setTitle("Updated Title");

        when(tripService.updateTrip(anyString(), anyString(), any(UpdateTripRequest.class)))
                .thenReturn(testTripDTO);

        mockMvc.perform(put("/api/trips/" + testTripId)
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testTripId));
    }

    @Test
    void testUpdateTripShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        UpdateTripRequest request = new UpdateTripRequest();
        request.setTitle("Updated Title");

        mockMvc.perform(put("/api/trips/" + testTripId)
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteTripShouldReturnOk() throws Exception {
        doNothing().when(tripService).deleteTrip(anyString(), anyString());

        mockMvc.perform(delete("/api/trips/" + testTripId)
                .param("userId", testUserId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Trip deleted successfully"));
    }

    @Test
    void testDeleteTripShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/trips/" + testTripId)
                .param("userId", testUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetTripShouldReturnOk() throws Exception {
        when(tripService.getTrip(anyString(), anyString())).thenReturn(testTripDTO);

        mockMvc.perform(get("/api/trips/" + testTripId)
                .param("userId", testUserId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testTripId))
                .andExpect(jsonPath("$.data.title").value("Test Trip"));
    }

    @Test
    void testGetTripShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/trips/" + testTripId)
                .param("userId", testUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserTripsShouldReturnOk() throws Exception {
        List<TripDTO> trips = new ArrayList<>();
        trips.add(testTripDTO);

        when(tripService.getUserTrips(anyString())).thenReturn(trips);

        mockMvc.perform(get("/api/trips/mine")
                .param("userId", testUserId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(testTripId));
    }

    @Test
    void testGetUserTripsShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/trips/mine")
                .param("userId", testUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddSpotShouldReturnOk() throws Exception {
        Long surfSpotId = 1L;

        doNothing().when(tripService).addSpot(anyString(), anyString(), anyLong());

        mockMvc.perform(post("/api/trips/" + testTripId + "/spots/" + surfSpotId)
                .param("userId", testUserId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Spot added to trip"));
    }

    @Test
    void testAddSpotShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        Long surfSpotId = 1L;

        mockMvc.perform(post("/api/trips/" + testTripId + "/spots/" + surfSpotId)
                .param("userId", testUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRemoveSpotShouldReturnOk() throws Exception {
        String tripSpotId = UUID.randomUUID().toString();
        doNothing().when(tripService).removeSpot(anyString(), anyString(), anyString());

        mockMvc.perform(delete("/api/trips/" + testTripId + "/spots/" + tripSpotId)
                .param("userId", testUserId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Spot removed from trip"));
    }

    @Test
    void testRemoveSpotShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        String tripSpotId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/trips/" + testTripId + "/spots/" + tripSpotId)
                .param("userId", testUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddMemberShouldReturnOk() throws Exception {
        AddTripMemberRequest request = new AddTripMemberRequest();
        request.setUserId("member-user-id");

        doNothing().when(tripService).addMember(anyString(), anyString(), any(AddTripMemberRequest.class));

        mockMvc.perform(post("/api/trips/" + testTripId + "/members")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Invitation sent"));
    }

    @Test
    void testAddMemberShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        AddTripMemberRequest request = new AddTripMemberRequest();
        request.setUserId("member-user-id");

        mockMvc.perform(post("/api/trips/" + testTripId + "/members")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRemoveMemberShouldReturnOk() throws Exception {
        String memberUserId = UUID.randomUUID().toString();
        doNothing().when(tripService).removeMember(anyString(), anyString(), anyString());

        mockMvc.perform(delete("/api/trips/" + testTripId + "/members/" + memberUserId)
                .param("currentUserId", testUserId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Member removed from trip"));
    }

    @Test
    void testRemoveMemberShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        String memberUserId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/trips/" + testTripId + "/members/" + memberUserId)
                .param("currentUserId", testUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUploadUrlShouldReturnOk() throws Exception {
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("image/jpeg");

        when(tripService.getUploadUrl(anyString(), anyString(), any(UploadMediaRequest.class), anyString()))
                .thenReturn("https://example.com/upload/123");

        mockMvc.perform(post("/api/trips/" + testTripId + "/media/upload-url")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadUrl").exists())
                .andExpect(jsonPath("$.data.mediaId").exists());
    }

    @Test
    void testGetUploadUrlShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        UploadMediaRequest request = new UploadMediaRequest();
        request.setMediaType("image/jpeg");

        mockMvc.perform(post("/api/trips/" + testTripId + "/media/upload-url")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRecordMediaShouldReturnOk() throws Exception {
        RecordMediaRequest request = new RecordMediaRequest();
        request.setMediaId("media-id-123");
        request.setUrl("https://example.com/media/123");
        request.setMediaType("image/jpeg");

        doNothing().when(tripService).recordMedia(anyString(), anyString(), any(RecordMediaRequest.class));

        mockMvc.perform(post("/api/trips/" + testTripId + "/media")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Media recorded successfully"));
    }

    @Test
    void testRecordMediaShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        RecordMediaRequest request = new RecordMediaRequest();
        request.setMediaId("media-id-123");
        request.setUrl("https://example.com/media/123");
        request.setMediaType("image/jpeg");

        mockMvc.perform(post("/api/trips/" + testTripId + "/media")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteMediaShouldReturnOk() throws Exception {
        String mediaId = UUID.randomUUID().toString();
        doNothing().when(tripService).deleteMedia(anyString(), anyString(), anyString());

        mockMvc.perform(delete("/api/trips/" + testTripId + "/media/" + mediaId)
                .param("userId", testUserId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Media deleted successfully"));
    }

    @Test
    void testDeleteMediaShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        String mediaId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/trips/" + testTripId + "/media/" + mediaId)
                .param("userId", testUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateTripShouldHandleServiceException() throws Exception {
        CreateTripRequest request = new CreateTripRequest();
        request.setTitle("New Trip");

        when(tripService.createTrip(anyString(), any(CreateTripRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(post("/api/trips")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isNotFound());
    }
}