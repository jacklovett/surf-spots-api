package com.lovettj.surfspotsapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.dto.SurfboardDTO;
import com.lovettj.surfspotsapi.dto.SurfboardMediaDTO;
import com.lovettj.surfspotsapi.requests.CreateSurfboardMediaRequest;
import com.lovettj.surfspotsapi.requests.CreateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UpdateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UploadSurfboardMediaRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.service.SurfboardService;
import com.lovettj.surfspotsapi.testutil.BaseControllerTest;
import com.lovettj.surfspotsapi.testutil.MockMvcDefaults;
import com.lovettj.surfspotsapi.testutil.SessionTestCookieFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SurfboardControllerTests extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SurfboardService surfboardService;

    private String testUserId;
    private String testSurfboardId;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        testSurfboardId = UUID.randomUUID().toString();
        objectMapper = new ObjectMapper();
    }

    private Cookie createValidSessionCookie() {
        return SessionTestCookieFactory.createSignedSessionCookie(testUserId);
    }

    @Test
    void testGetUploadUrlShouldReturnOk() throws Exception {
        UploadSurfboardMediaRequest request = new UploadSurfboardMediaRequest();
        request.setMediaType("image");

        when(surfboardService.getUploadUrl(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("https://example.com/upload/123");

        mockMvc.perform(post("/api/surfboards/" + testSurfboardId + "/media/upload-url")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadUrl").exists())
                .andExpect(jsonPath("$.data.mediaId").exists());
    }

    @Test
    void testGetUploadUrlShouldReturnServiceUnavailableWhenStorageNotConfigured() throws Exception {
        UploadSurfboardMediaRequest request = new UploadSurfboardMediaRequest();
        request.setMediaType("image");

        when(surfboardService.getUploadUrl(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Media storage is not configured."));

        mockMvc.perform(post("/api/surfboards/" + testSurfboardId + "/media/upload-url")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ApiErrors.MEDIA_UPLOAD_UNAVAILABLE));
    }

    @Test
    void testGetUploadUrlShouldReturnServiceUnavailableWithSafeMessageWhenUnexpectedError() throws Exception {
        UploadSurfboardMediaRequest request = new UploadSurfboardMediaRequest();
        request.setMediaType("image");

        when(surfboardService.getUploadUrl(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Internal error"));

        mockMvc.perform(post("/api/surfboards/" + testSurfboardId + "/media/upload-url")
                .param("userId", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ApiErrors.MEDIA_UPLOAD_UNAVAILABLE));
    }

    // --- GET /api/surfboards ---

    @Test
    void testGetUserSurfboardsShouldReturnListWhenAuthenticated() throws Exception {
        SurfboardDTO surfboard = SurfboardDTO.builder()
                .id(testSurfboardId)
                .userId(testUserId)
                .name("My Board")
                .boardType("Shortboard")
                .build();

        when(surfboardService.getUserSurfboards(testUserId)).thenReturn(List.of(surfboard));

        mockMvc.perform(get("/api/surfboards")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(testSurfboardId))
                .andExpect(jsonPath("$.data[0].name").value("My Board"));

        verify(surfboardService).getUserSurfboards(testUserId);
    }

    @Test
    void testGetUserSurfboardsShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/surfboards"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserSurfboardsShouldReturn500WhenServiceThrows() throws Exception {
        when(surfboardService.getUserSurfboards(anyString()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/surfboards")
                .cookie(createValidSessionCookie()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // --- POST /api/surfboards ---

    @Test
    void testCreateSurfboardShouldReturnCreatedWhenAuthenticated() throws Exception {
        CreateSurfboardRequest request = new CreateSurfboardRequest();
        request.setName("New Board");
        request.setBoardType("shortboard");

        SurfboardDTO created = SurfboardDTO.builder()
                .id(testSurfboardId)
                .userId(testUserId)
                .name("New Board")
                .boardType("Shortboard")
                .build();

        when(surfboardService.createSurfboard(anyString(), any(CreateSurfboardRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/surfboards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(testSurfboardId))
                .andExpect(jsonPath("$.data.name").value("New Board"));
    }

    @Test
    void testCreateSurfboardShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        CreateSurfboardRequest request = new CreateSurfboardRequest();
        request.setName("New Board");

        mockMvc.perform(post("/api/surfboards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateSurfboardShouldReturn500WhenServiceThrows() throws Exception {
        CreateSurfboardRequest request = new CreateSurfboardRequest();
        request.setName("Board");

        when(surfboardService.createSurfboard(anyString(), any(CreateSurfboardRequest.class)))
                .thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(post("/api/surfboards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // --- GET /api/surfboards/{surfboardId} ---

    @Test
    void testGetSurfboardShouldReturnOkWhenAuthenticated() throws Exception {
        SurfboardDTO surfboard = SurfboardDTO.builder()
                .id(testSurfboardId)
                .userId(testUserId)
                .name("My Board")
                .build();

        when(surfboardService.getSurfboard(testUserId, testSurfboardId)).thenReturn(surfboard);

        mockMvc.perform(get("/api/surfboards/" + testSurfboardId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testSurfboardId));

        verify(surfboardService).getSurfboard(testUserId, testSurfboardId);
    }

    @Test
    void testGetSurfboardShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/surfboards/" + testSurfboardId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetSurfboardShouldReturn404WhenNotFound() throws Exception {
        when(surfboardService.getSurfboard(anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Surfboard not found"));

        mockMvc.perform(get("/api/surfboards/" + testSurfboardId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Surfboard not found"));
    }

    // --- PUT /api/surfboards/{surfboardId} ---

    @Test
    void testUpdateSurfboardShouldReturnOkWhenAuthenticated() throws Exception {
        UpdateSurfboardRequest request = new UpdateSurfboardRequest();
        request.setName("Updated Board");

        SurfboardDTO updated = SurfboardDTO.builder()
                .id(testSurfboardId)
                .userId(testUserId)
                .name("Updated Board")
                .build();

        when(surfboardService.updateSurfboard(anyString(), anyString(), any(UpdateSurfboardRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/surfboards/" + testSurfboardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Board"));
    }

    @Test
    void testUpdateSurfboardShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        UpdateSurfboardRequest request = new UpdateSurfboardRequest();
        request.setName("Updated Board");

        mockMvc.perform(put("/api/surfboards/" + testSurfboardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateSurfboardShouldReturn404WhenNotFound() throws Exception {
        UpdateSurfboardRequest request = new UpdateSurfboardRequest();
        request.setName("Updated Board");

        when(surfboardService.updateSurfboard(anyString(), anyString(), any(UpdateSurfboardRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Surfboard not found"));

        mockMvc.perform(put("/api/surfboards/" + testSurfboardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/surfboards/{surfboardId} ---

    @Test
    void testDeleteSurfboardShouldReturnOkWhenAuthenticated() throws Exception {
        doNothing().when(surfboardService).deleteSurfboard(anyString(), anyString());

        mockMvc.perform(delete("/api/surfboards/" + testSurfboardId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Surfboard deleted successfully"));

        verify(surfboardService).deleteSurfboard(testUserId, testSurfboardId);
    }

    @Test
    void testDeleteSurfboardShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/surfboards/" + testSurfboardId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteSurfboardShouldReturn403WhenUserDoesNotOwnSurfboard() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this surfboard"))
                .when(surfboardService).deleteSurfboard(anyString(), anyString());

        mockMvc.perform(delete("/api/surfboards/" + testSurfboardId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Not authorized to delete this surfboard"));
    }

    // --- POST /api/surfboards/{surfboardId}/media ---

    @Test
    void testAddMediaShouldReturnCreatedWhenAuthenticated() throws Exception {
        CreateSurfboardMediaRequest request = new CreateSurfboardMediaRequest();
        request.setMediaId(UUID.randomUUID().toString());
        request.setOriginalUrl("https://example.com/media/original.jpg");
        request.setThumbUrl("https://example.com/media/thumb.jpg");
        request.setMediaType("image/jpeg");

        SurfboardMediaDTO mediaDTO = SurfboardMediaDTO.builder()
                .id(request.getMediaId())
                .surfboardId(testSurfboardId)
                .originalUrl(request.getOriginalUrl())
                .mediaType("image/jpeg")
                .build();

        when(surfboardService.addMedia(anyString(), anyString(), any(CreateSurfboardMediaRequest.class)))
                .thenReturn(mediaDTO);

        mockMvc.perform(post("/api/surfboards/" + testSurfboardId + "/media")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(createValidSessionCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(request.getMediaId()));
    }

    @Test
    void testAddMediaShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        CreateSurfboardMediaRequest request = new CreateSurfboardMediaRequest();
        request.setMediaId(UUID.randomUUID().toString());
        request.setOriginalUrl("https://example.com/photo.jpg");
        request.setMediaType("image/jpeg");

        mockMvc.perform(post("/api/surfboards/" + testSurfboardId + "/media")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /api/surfboards/media/{mediaId} ---

    @Test
    void testDeleteMediaShouldReturnOkWhenAuthenticated() throws Exception {
        String mediaId = UUID.randomUUID().toString();
        doNothing().when(surfboardService).deleteMedia(anyString(), anyString());

        mockMvc.perform(delete("/api/surfboards/media/" + mediaId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isOk());

        verify(surfboardService).deleteMedia(testUserId, mediaId);
    }

    @Test
    void testDeleteMediaShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        String mediaId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/surfboards/media/" + mediaId))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteMediaShouldReturn404WhenMediaNotFound() throws Exception {
        String mediaId = UUID.randomUUID().toString();

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"))
                .when(surfboardService).deleteMedia(anyString(), anyString());

        mockMvc.perform(delete("/api/surfboards/media/" + mediaId)
                .cookie(createValidSessionCookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Media not found"));
    }
}
