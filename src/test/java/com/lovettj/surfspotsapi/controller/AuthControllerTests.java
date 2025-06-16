package com.lovettj.surfspotsapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.entity.AuthProvider;
import com.lovettj.surfspotsapi.requests.AuthRequest;
import com.lovettj.surfspotsapi.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerUserShouldReturnSuccess() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");
        authRequest.setProvider(AuthProvider.EMAIL);
        authRequest.setName("Test User");

        doNothing().when(userService).registerUser(any(AuthRequest.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"message\":\"Account created successfully!\",\"status\":201}"));
    }

    @Test
    void registerUserShouldReturnSuccessForGoogleAuth() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("google@example.com");
        authRequest.setProvider(AuthProvider.GOOGLE);
        authRequest.setProviderId("google123");
        authRequest.setName("Google User");

        doNothing().when(userService).registerUser(any(AuthRequest.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"message\":\"Account created successfully!\",\"status\":201}"));
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
                .andExpect(content().json("{\"message\":\"An account with this email already exists. Please try signing in instead.\",\"status\":409}"));
    }

    @Test
    void registerUserShouldReturnBadRequestWhenPasswordTooShort() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("short");
        authRequest.setProvider(AuthProvider.EMAIL);
        authRequest.setName("Test User");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, 
            "Password must be at least 8 characters"))
            .when(userService).registerUser(any(AuthRequest.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"message\":\"Password must be at least 8 characters\",\"status\":400}"));
    }

    @Test
    void registerUserShouldReturnBadRequestWhenPasswordMissingForEmailProvider() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setProvider(AuthProvider.EMAIL);
        authRequest.setName("Test User");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, 
            "Password must be at least 8 characters"))
            .when(userService).registerUser(any(AuthRequest.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"message\":\"Password must be at least 8 characters\",\"status\":400}"));
    }

    @Test
    void registerUserShouldReturnBadRequestWhenProviderIdMissingForGoogleProvider() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("google@example.com");
        authRequest.setProvider(AuthProvider.GOOGLE);
        authRequest.setName("Google User");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, 
            "Provider ID is required for social authentication"))
            .when(userService).registerUser(any(AuthRequest.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"message\":\"Provider ID is required for social authentication\",\"status\":400}"));
    }
} 