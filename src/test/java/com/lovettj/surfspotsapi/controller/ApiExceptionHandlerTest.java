package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.enums.WaveSize;
import com.lovettj.surfspotsapi.requests.SurfSpotRequest;
import com.lovettj.surfspotsapi.requests.StartLiveSurfSessionRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.service.SurfSessionService;
import com.lovettj.surfspotsapi.service.SurfSpotService;
import com.lovettj.surfspotsapi.service.UserSurfSpotService;
import com.lovettj.surfspotsapi.testutil.BaseControllerTest;
import com.lovettj.surfspotsapi.testutil.SessionTestCookieFactory;

/**
 * Contract tests for {@link ApiExceptionHandler}: failures must return {@code ApiResponse} with
 * {@code success: false} and a user-facing {@code message}.
 */
class ApiExceptionHandlerTest extends BaseControllerTest {

    private static final String TEST_USER_ID = "user-validation-test";
    private static final String INVALID_DIRECTION_MESSAGE =
            "Invalid direction. Use N, NE, S, etc., or a range like NE - SE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SurfSessionService surfSessionService;

    @MockBean
    private SurfSpotService surfSpotService;

    @MockBean
    private UserSurfSpotService userSurfSpotService;

    @Test
    void invalidStartLiveSessionBodyShouldReturnApiResponseWithConstraintMessage() throws Exception {
        StartLiveSurfSessionRequest request = new StartLiveSurfSessionRequest();
        request.setStartLatitude(54.4783);
        request.setStartLongitude(-8.2779);
        request.setShareLocationWithEmergencyContact(true);

        mockMvc.perform(post("/api/surf-sessions/start")
                        .cookie(SessionTestCookieFactory.createSignedSessionCookie(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value(ApiErrors.LIVE_SESSION_EXPECTED_RETURN_REQUIRED));

        verify(surfSessionService, never()).startLiveSession(anyString(), any(StartLiveSurfSessionRequest.class));
    }

    @Test
    void invalidSurfSessionUpdateBodyShouldReturnApiResponseWithConstraintMessage() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("surfSpotId", 10L);
        body.put("waveSize", WaveSize.SMALL.name());

        mockMvc.perform(put("/api/surf-sessions/2")
                        .cookie(SessionTestCookieFactory.createSignedSessionCookie(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value(ApiErrors.SESSION_DATE_OR_START_INSTANT_REQUIRED));

        verify(surfSessionService, never()).updateSession(anyString(), anyLong(), any());
    }

    @Test
    void entityConstraintViolationOnPersistShouldReturnApiResponseWithConstraintMessage() throws Exception {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn(INVALID_DIRECTION_MESSAGE);
        ConstraintViolationException constraintViolationException =
                new ConstraintViolationException(Set.of(violation));
        when(surfSpotService.createSurfSpot(any(SurfSpotRequest.class)))
                .thenThrow(constraintViolationException);

        mockMvc.perform(post("/api/surf-spots/management")
                        .cookie(SessionTestCookieFactory.createSignedSessionCookie(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Pipeline",
                              "description": "A famous surf spot.",
                              "regionId": 1,
                              "latitude": 0.2,
                              "longitude": 0.1,
                              "status": "Pending",
                              "type": "Reef Break",
                              "beachBottomType": "Rock",
                              "skillLevel": "Intermediate",
                              "waveDirection": "Left",
                              "swellDirection": "N",
                              "windDirection": "SW",
                              "tide": "Mid"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value(INVALID_DIRECTION_MESSAGE));
    }

    @Test
    void responseStatusExceptionShouldReturnApiResponseWithServiceMessage() throws Exception {
        when(surfSpotService.updateSurfSpot(anyLong(), any(SurfSpotRequest.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You can only update surf spots you created"));

        mockMvc.perform(patch("/api/surf-spots/management/1")
                        .cookie(SessionTestCookieFactory.createSignedSessionCookie(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Pipeline",
                              "description": "Updated",
                              "regionId": 1,
                              "latitude": 0.2,
                              "longitude": 0.1,
                              "status": "Pending",
                              "type": "Reef Break",
                              "beachBottomType": "Rock",
                              "skillLevel": "Intermediate",
                              "waveDirection": "Left",
                              "swellDirection": "N",
                              "windDirection": "SW",
                              "tide": "Mid"
                            }
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You can only update surf spots you created"));
    }

    @Test
    void unexpectedFailureShouldReturnActionSpecificServerErrorMessage() throws Exception {
        when(surfSpotService.createSurfSpot(any(SurfSpotRequest.class)))
                .thenThrow(new RuntimeException("database connection lost"));

        mockMvc.perform(post("/api/surf-spots/management")
                        .cookie(SessionTestCookieFactory.createSignedSessionCookie(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Pipeline",
                              "description": "A famous surf spot.",
                              "regionId": 1,
                              "latitude": 0.2,
                              "longitude": 0.1,
                              "status": "Pending",
                              "type": "Reef Break",
                              "beachBottomType": "Rock",
                              "skillLevel": "Intermediate",
                              "waveDirection": "Left",
                              "swellDirection": "N",
                              "windDirection": "SW",
                              "tide": "Mid"
                            }
                            """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(ApiErrors.formatErrorMessage("create", "surf spot")));
    }

    @Test
    void invalidPathVariableTypeShouldReturnBadRequestWithSafeMessage() throws Exception {
        mockMvc.perform(delete("/api/user-spots/remove/abc")
                        .cookie(SessionTestCookieFactory.createSignedSessionCookie(TEST_USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(ApiErrors.CHECK_INPUT));

        verify(userSurfSpotService, never()).removeUserSurfSpot(anyString(), anyLong());
    }
}
