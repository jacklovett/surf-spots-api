package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.entity.PasswordResetToken;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.TokenRepository;
import com.lovettj.surfspotsapi.security.RateLimiter;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTests {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private UserService userService;

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken testToken;
    private static final String VALID_ORIGIN = "http://localhost:5173";
    private static final String INVALID_ORIGIN = "http://malicious.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("test-user-id-123")
                .email("test@example.com")
                .name("Test User")
                .build();

        testToken = new PasswordResetToken("valid-token", testUser);
    }

    @Test
    void testCreatePasswordResetTokenSuccess() {
        when(userService.findUserByEmail("test@example.com")).thenReturn(testUser);
        doNothing().when(rateLimiter).checkRateLimit("test@example.com");

        assertDoesNotThrow(()
                -> passwordResetService.createPasswordResetToken("test@example.com", VALID_ORIGIN)
        );

        verify(tokenRepository).deleteByUser(testUser);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendEmail(eq("test@example.com"),
                eq("Password Reset Request"),
                eq("reset-password"),
                anyMap());
    }

    @Test
    void testCreatePasswordResetTokenInvalidOrigin() {
        doNothing().when(rateLimiter).checkRateLimit("test@example.com");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passwordResetService.createPasswordResetToken("test@example.com", INVALID_ORIGIN)
        );

        assertEquals("Invalid origin", exception.getReason());
        verify(userService, never()).findUserByEmail(any());
    }

    @Test
    void testCreatePasswordResetTokenRateLimitExceeded() {
        // Mock the rate limiter to throw an exception
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
                .when(rateLimiter).checkRateLimit("test@example.com");

        // Capture the exception
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passwordResetService.createPasswordResetToken("test@example.com", VALID_ORIGIN));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        verify(userService, never()).findUserByEmail(any());
    }

    @Test
    void testResetPasswordSuccess() {

        testToken.setExpiresAt(Instant.now().plusSeconds(3600)); // Set expiry 1 hour in future
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(testToken));

        assertDoesNotThrow(()
                -> passwordResetService.resetPassword("valid-token", "newPassword")
        );

        verify(userService).setUserPassword(testUser, "newPassword");
        verify(tokenRepository).delete(testToken);
    }

    @Test
    void testResetPasswordInvalidToken() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passwordResetService.resetPassword("invalid-token", "newPassword")
        );

        assertEquals("Invalid token", exception.getReason());
        verify(userService, never()).setUserPassword(any(), any());
    }

    @Test
    void testResetPasswordExpiredToken() {
        testToken.setExpiresAt(Instant.now().minusSeconds(3600)); // Set expiry 1 hour in past
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(testToken));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passwordResetService.resetPassword("expired-token", "newPassword")
        );

        assertEquals("Token has expired", exception.getReason());
        verify(userService, never()).setUserPassword(any(), any());
    }
}
