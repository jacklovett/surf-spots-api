package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.service.UserService;
import com.lovettj.surfspotsapi.testutil.MockMvcDefaults;
import com.lovettj.surfspotsapi.testutil.SessionTestCookieFactory;

/**
 * Focused tests for the session-minimisation endpoint. The session cookie only
 * carries {id, email, name}; this endpoint is what pages call when they need
 * the richer profile, so it must reliably derive identity from the signed
 * cookie and never from a query parameter.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcDefaults.class)
class UserControllerMeTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getCurrentUserShouldReturnProfileForAuthenticatedCaller() throws Exception {
        String userId = "test-user-id";
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setName("Test User");
        Settings settings = new Settings();
        settings.setNewSurfSpotEmails(false);
        user.setSettings(settings);

        when(userService.getUserProfile(userId)).thenReturn(Optional.of(new UserProfile(user)));

        mockMvc.perform(get("/api/user/me")
                .cookie(SessionTestCookieFactory.createSignedSessionCookie(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", Matchers.is("test@example.com")))
                .andExpect(jsonPath("$.data.name", Matchers.is("Test User")));
    }

    @Test
    void getCurrentUserShouldReturn403WhenNoSessionCookie() throws Exception {
        // No cookie: Spring Security leaves the request anonymous; access to a
        // protected route is denied with 403 (not 401) with the default setup.
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCurrentUserShouldReturn404WhenProfileMissing() throws Exception {
        // Edge case: the session cookie survived a user deletion. We must not 500
        // or 200 with a half-populated body — the frontend treats 404 as "log me
        // out" and that's the right behaviour here.
        String userId = "ghost-user";
        when(userService.getUserProfile(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/user/me")
                .cookie(SessionTestCookieFactory.createSignedSessionCookie(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.USER_NOT_FOUND)));
    }

    @Test
    void getCurrentUserShouldReturn500WhenServiceFailsUnexpectedly() throws Exception {
        // Any unexpected exception must be translated into a generic "couldn't
        // load profile" message so we never leak stack traces or internal detail.
        String userId = "test-user-id";
        doThrow(new RuntimeException("db down")).when(userService).getUserProfile(any());

        mockMvc.perform(get("/api/user/me")
                .cookie(SessionTestCookieFactory.createSignedSessionCookie(userId)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.formatErrorMessage("load", "profile"))));
    }
}
