package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.lovettj.surfspotsapi.dto.UserProfile;
import com.lovettj.surfspotsapi.entity.AuthProvider;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.security.RateLimiter;
import com.lovettj.surfspotsapi.service.PasswordResetService;
import com.lovettj.surfspotsapi.service.UserService;
import com.lovettj.surfspotsapi.testutil.MockMvcDefaults;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcDefaults.class)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private RateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerUserShouldReturnSuccess() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");
        authRequest.setProvider(AuthProvider.EMAIL);
        authRequest.setName("Test User");

        Settings settings = new Settings();
        settings.setNewSurfSpotEmails(false);

        User user = new User();
        user.setId("test-user-id");
        user.setEmail("test@example.com");
        user.setSettings(settings);

        when(userService.registerUser(any(AuthRequest.class))).thenReturn(user);

        UserProfile expectedProfile = new UserProfile(user);
        String expectedJson = objectMapper.writeValueAsString(expectedProfile);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/user/test-user-id"))
                .andExpect(content().json(
                        "{\"message\":\"Account created successfully\",\"status\":201,\"success\":true," +
                                "\"data\":" + expectedJson + "}"));
    }

    @Test
    void registerUserShouldReturnSuccessForGoogleAuth() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("google@example.com");
        authRequest.setProvider(AuthProvider.GOOGLE);
        authRequest.setProviderId("google123");
        authRequest.setName("Google User");

        Settings settings = new Settings();
        settings.setNewSurfSpotEmails(false);

        User user = new User();
        user.setId("test-user-id");
        user.setEmail("google@example.com");
        user.setSettings(settings);

        when(userService.registerUser(any(AuthRequest.class))).thenReturn(user);

        UserProfile expectedProfile = new UserProfile(user);
        String expectedJson = objectMapper.writeValueAsString(expectedProfile);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/user/test-user-id"))
                .andExpect(content().json(
                        "{\"message\":\"Account created successfully\",\"status\":201,\"success\":true," +
                                "\"data\":" + expectedJson + "}"));
    }

    @Test
    void registerUserShouldReturnSuccessForFacebookAuth() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("facebook@example.com");
        authRequest.setProvider(AuthProvider.FACEBOOK);
        authRequest.setProviderId("facebook123");
        authRequest.setName("Facebook User");

        Settings settings = new Settings();
        settings.setNewSurfSpotEmails(false);

        User user = new User();
        user.setId("test-user-id");
        user.setEmail("facebook@example.com");
        user.setSettings(settings);

        when(userService.registerUser(any(AuthRequest.class))).thenReturn(user);

        UserProfile expectedProfile = new UserProfile(user);
        String expectedJson = objectMapper.writeValueAsString(expectedProfile);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/user/test-user-id"))
                .andExpect(content().json(
                        "{\"message\":\"Account created successfully\",\"status\":201,\"success\":true," +
                                "\"data\":" + expectedJson + "}"));
    }

    @Test
    void registerUserShouldReturnConflictWhenEmailExists() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("existing@example.com");
        authRequest.setPassword("password123");
        authRequest.setProvider(AuthProvider.EMAIL);
        authRequest.setName("Existing User");

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "An account with this email already exists. Please try signing in instead."))
                .when(userService).registerUser(any(AuthRequest.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().json(
                        "{\"message\":\"An account with this email already exists. Please try signing in instead.\",\"status\":409}"));
    }

    @Test
    void registerUserShouldReturnBadRequestWhenPasswordPolicyFails() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("short");
        authRequest.setProvider(AuthProvider.EMAIL);
        authRequest.setName("Test User");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.PASSWORD_POLICY_VIOLATION))
                .when(userService).registerUser(any(AuthRequest.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.PASSWORD_POLICY_VIOLATION)));
    }

    @Test
    void registerUserShouldReturnBadRequestWhenProviderIdMissingForGoogleProvider() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("google@example.com");
        authRequest.setProvider(AuthProvider.GOOGLE);
        authRequest.setName("Google User");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A Provider Id is required for OAuth providers."))
                .when(userService).registerUser(any(AuthRequest.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        "{\"message\":\"A Provider Id is required for OAuth providers.\",\"status\":400}"));
    }

    @Test
    void registerUserShouldReturnInternalServerErrorWithSafeMessageWhenUnexpectedError() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");
        authRequest.setProvider(AuthProvider.EMAIL);
        authRequest.setName("Test User");

        doThrow(new RuntimeException("Internal database error"))
                .when(userService).registerUser(any(AuthRequest.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(
                        "{\"message\":\"" + ApiErrors.formatErrorMessage("create", "account")
                                + "\",\"status\":500,\"success\":false}"));
    }

    @Test
    void registerUserShouldReturn403WhenOriginIsMissing() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");
        authRequest.setProvider(AuthProvider.EMAIL);
        authRequest.setName("Test User");

        mockMvc.perform(post("/api/auth/register")
                .with(MockMvcDefaults.stripOrigin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.INVALID_ORIGIN)));
    }

    @Test
    void loginUserShouldReturnGenericMessageOnUnknownEmail() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("unknown@example.com");
        authRequest.setPassword("password123");
        authRequest.setProvider(AuthProvider.EMAIL);

        // Simulate the service returning the same 401 it returns for a wrong password.
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, ApiErrors.INVALID_CREDENTIALS))
                .when(userService).loginUser(any(), any());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.INVALID_CREDENTIALS)));
    }

    @Test
    void loginUserShouldMapServiceNotFoundTo401WithGenericMessage() throws Exception {
        // Defence-in-depth: even if a stray 404 slipped out of the service layer, the
        // controller must translate it to a 401 with the generic credentials message.
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("unknown@example.com");
        authRequest.setPassword("password123");
        authRequest.setProvider(AuthProvider.EMAIL);

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(userService).loginUser(any(), any());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.INVALID_CREDENTIALS)));
    }

    @Test
    void forgotPasswordShouldReturnGenericResponseRegardlessOfEmail() throws Exception {
        String body = "{\"email\":\"anyone@example.com\"}";

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.FORGOT_PASSWORD_ACCEPTED)));
    }

    @Test
    void forgotPasswordShouldReturnGenericResponseEvenWhenServiceFails() throws Exception {
        // The service throwing a 500 must not expose the failure to callers — it
        // would give an attacker a side channel for account enumeration.
        String body = "{\"email\":\"anyone@example.com\"}";

        doThrow(new RuntimeException("boom")).when(passwordResetService)
                .createPasswordResetToken(any(), any(), any());

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.FORGOT_PASSWORD_ACCEPTED)));
    }

    @Test
    void forgotPasswordShouldSurfaceRateLimit() throws Exception {
        String body = "{\"email\":\"anyone@example.com\"}";

        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ApiErrors.TOO_MANY_ATTEMPTS))
                .when(passwordResetService).createPasswordResetToken(any(), any(), any());

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", Matchers.is(ApiErrors.TOO_MANY_ATTEMPTS)));
    }
}
