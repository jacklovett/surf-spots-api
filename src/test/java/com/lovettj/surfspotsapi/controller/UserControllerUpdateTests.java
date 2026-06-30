package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.requests.ChangePasswordRequest;
import com.lovettj.surfspotsapi.requests.SettingsRequest;
import com.lovettj.surfspotsapi.requests.UserRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.service.UserService;
import com.lovettj.surfspotsapi.testutil.BaseControllerTest;
import com.lovettj.surfspotsapi.testutil.MockMvcDefaults;
import com.lovettj.surfspotsapi.testutil.SessionTestCookieFactory;

import jakarta.servlet.http.Cookie;

class UserControllerUpdateTests extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID = "test-user-id";

    private Cookie sessionCookie() {
        return SessionTestCookieFactory.createSignedSessionCookie(TEST_USER_ID);
    }

    // --- PUT /api/user/update/profile ---

    @Test
    void testUpdateProfileShouldReturnOkWhenAuthenticated() throws Exception {
        UserRequest request = new UserRequest();
        request.setName("Updated Name");
        request.setEmail("updated@example.com");

        mockMvc.perform(put("/api/user/update/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Profile updated successfully!"));

        verify(userService).updateUserProfile(TEST_USER_ID, request);
    }

    @Test
    void testUpdateProfileShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        UserRequest request = new UserRequest();
        request.setName("Test User");

        mockMvc.perform(put("/api/user/update/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateProfileShouldReturn404WhenUserNotFound() throws Exception {
        UserRequest request = new UserRequest();
        request.setName("Ghost");

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.USER_NOT_FOUND))
                .when(userService).updateUserProfile(anyString(), any(UserRequest.class));

        mockMvc.perform(put("/api/user/update/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ApiErrors.USER_NOT_FOUND));
    }

    @Test
    void testUpdateProfileShouldReturn500WhenServiceThrowsUnexpectedly() throws Exception {
        UserRequest request = new UserRequest();
        request.setName("Test");

        doThrow(new RuntimeException("DB failure")).when(userService).updateUserProfile(anyString(), any(UserRequest.class));

        mockMvc.perform(put("/api/user/update/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ApiErrors.formatErrorMessage("update", "profile")));
    }

    @Test
    void testUpdateProfileShouldAcceptEmergencyContactEmail() throws Exception {
        UserRequest request = new UserRequest();
        request.setName("Test User");
        request.setEmergencyContactName("Jane Doe");
        request.setEmergencyContactEmail("jane@example.com");

        mockMvc.perform(put("/api/user/update/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Profile updated successfully!"));

        verify(userService).updateUserProfile(TEST_USER_ID, request);
    }

    // --- PUT /api/user/update-password ---

    @Test
    void testUpdatePasswordShouldReturnOkWhenAuthenticated() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword1!");
        request.setNewPassword("newPassword1!");

        doNothing().when(userService).updatePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/user/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Password changed successfully!"));
    }

    @Test
    void testUpdatePasswordShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword1!");
        request.setNewPassword("newPassword1!");

        mockMvc.perform(put("/api/user/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdatePasswordShouldReturn400WhenCurrentPasswordIsWrong() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword1!");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect"))
                .when(userService).updatePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/user/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    void testUpdatePasswordShouldReturn500WhenServiceThrowsUnexpectedly() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("any");
        request.setNewPassword("any");

        doThrow(new RuntimeException("DB error")).when(userService).updatePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/user/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ApiErrors.formatErrorMessage("change", "password")));
    }

    // --- PUT /api/user/settings ---

    @Test
    void testUpdateSettingsShouldReturnOkWhenAuthenticated() throws Exception {
        SettingsRequest request = new SettingsRequest();
        request.setNewSurfSpotEmails(true);
        request.setSwellSeasonEmails(false);

        doNothing().when(userService).updateSettings(any(SettingsRequest.class));

        mockMvc.perform(put("/api/user/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Settings updated successfully!"));
    }

    @Test
    void testUpdateSettingsShouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        SettingsRequest request = new SettingsRequest();
        request.setNewSurfSpotEmails(true);

        mockMvc.perform(put("/api/user/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateSettingsShouldReturn404WhenUserNotFound() throws Exception {
        SettingsRequest request = new SettingsRequest();

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.USER_NOT_FOUND))
                .when(userService).updateSettings(any(SettingsRequest.class));

        mockMvc.perform(put("/api/user/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ApiErrors.USER_NOT_FOUND));
    }

    @Test
    void testUpdateSettingsShouldReturn500WhenServiceThrowsUnexpectedly() throws Exception {
        SettingsRequest request = new SettingsRequest();

        doThrow(new RuntimeException("unexpected")).when(userService).updateSettings(any(SettingsRequest.class));

        mockMvc.perform(put("/api/user/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .cookie(sessionCookie()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ApiErrors.formatErrorMessage("update", "settings")));
    }
}
