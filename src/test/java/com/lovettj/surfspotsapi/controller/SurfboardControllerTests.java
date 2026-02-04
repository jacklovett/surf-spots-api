package com.lovettj.surfspotsapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.requests.UploadSurfboardMediaRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.service.SurfboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SurfboardControllerTests {

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
        return new Cookie("session", "testpayload.testsignature");
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
}
