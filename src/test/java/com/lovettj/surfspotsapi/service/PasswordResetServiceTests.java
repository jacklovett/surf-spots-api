package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.config.AllowedOrigins;
import com.lovettj.surfspotsapi.entity.PasswordResetToken;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.TokenRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.security.RateLimiter;
import com.lovettj.surfspotsapi.security.TokenHasher;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTests {

    private static final String VALID_ORIGIN = "http://localhost:5173";
    private static final String INVALID_ORIGIN = "http://malicious.com";
    private static final String CLIENT_IP = "203.0.113.42";
    private static final String USER_EMAIL = "test@example.com";

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private UserService userService;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private AllowedOrigins allowedOrigins;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("test-user-id-123")
                .email(USER_EMAIL)
                .name("Test User")
                .build();
    }

    @Test
    void createPasswordResetTokenShouldEmailHashedTokenOnSuccess() {
        when(allowedOrigins.contains(VALID_ORIGIN)).thenReturn(true);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> passwordResetService.createPasswordResetToken(USER_EMAIL, VALID_ORIGIN, CLIENT_IP));

        verify(rateLimiter).checkRateLimit(RateLimiter.Bucket.FORGOT_PASSWORD, CLIENT_IP);
        verify(rateLimiter).checkRateLimit(RateLimiter.Bucket.FORGOT_PASSWORD, USER_EMAIL);
        verify(tokenRepository).deleteByUser(testUser);

        // Only the hashed token is persisted; the plaintext is emailed. A plaintext
        // match in the DB would be a regression that lets an attacker with a read
        // leak trivially reset any account.
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        String persistedHash = tokenCaptor.getValue().getTokenHash();
        assertNotNull(persistedHash);
        assertEquals(64, persistedHash.length(), "SHA-256 hex should be 64 characters");

        verify(emailService).sendEmail(eq(USER_EMAIL),
                eq("Password Reset Request"),
                eq("reset-password"),
                anyMap());
    }

    @Test
    void createPasswordResetTokenShouldReturnSilentlyWhenEmailUnknown() {
        // No ResponseStatusException and no exception from the email service — the
        // caller-facing response must be indistinguishable from the happy path so
        // we don't leak which emails are registered.
        when(allowedOrigins.contains(VALID_ORIGIN)).thenReturn(true);
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.createPasswordResetToken("ghost@example.com", VALID_ORIGIN, CLIENT_IP));

        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        verify(tokenRepository, never()).deleteByUser(any());
        verify(emailService, never()).sendEmail(any(), any(), any(), anyMap());
    }

    @Test
    void createPasswordResetTokenShouldRejectUntrustedOrigin() {
        when(allowedOrigins.contains(INVALID_ORIGIN)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.createPasswordResetToken(USER_EMAIL, INVALID_ORIGIN, CLIENT_IP));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(ApiErrors.INVALID_ORIGIN, exception.getReason());
        // Crucially: we never hit the user lookup, so an attacker can't use a
        // bad-origin probe to learn whether an account exists via timing.
        verify(userRepository, never()).findByEmail(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void createPasswordResetTokenShouldSurfaceRateLimit() {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ApiErrors.TOO_MANY_ATTEMPTS))
                .when(rateLimiter).checkRateLimit(RateLimiter.Bucket.FORGOT_PASSWORD, CLIENT_IP);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.createPasswordResetToken(USER_EMAIL, VALID_ORIGIN, CLIENT_IP));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void resetPasswordShouldHashIncomingTokenAndConsumeIt() {
        String plaintext = "plaintext-reset-token";
        String hashed = TokenHasher.hash(plaintext);
        PasswordResetToken resetToken = new PasswordResetToken(hashed, testUser);
        resetToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(tokenRepository.findByTokenHash(hashed)).thenReturn(Optional.of(resetToken));

        assertDoesNotThrow(() -> passwordResetService.resetPassword(plaintext, "NewPassword123"));

        verify(userService).setUserPassword(testUser, "NewPassword123");
        verify(tokenRepository).delete(resetToken);
    }

    @Test
    void resetPasswordShouldRejectUnknownTokenWithGenericMessage() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.resetPassword("bogus", "NewPassword123"));

        assertEquals(ApiErrors.RESET_TOKEN_INVALID_OR_EXPIRED, exception.getReason());
        verify(userService, never()).setUserPassword(any(), any());
    }

    @Test
    void resetPasswordShouldRejectExpiredTokenAndDeleteIt() {
        String plaintext = "plaintext-reset-token";
        String hashed = TokenHasher.hash(plaintext);
        PasswordResetToken resetToken = new PasswordResetToken(hashed, testUser);
        resetToken.setExpiresAt(Instant.now().minusSeconds(60));

        when(tokenRepository.findByTokenHash(hashed)).thenReturn(Optional.of(resetToken));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.resetPassword(plaintext, "NewPassword123"));

        assertEquals(ApiErrors.RESET_TOKEN_INVALID_OR_EXPIRED, exception.getReason());
        verify(userService, never()).setUserPassword(any(), any());
        verify(tokenRepository).delete(resetToken);
    }

    @Test
    void resetPasswordShouldRejectBlankToken() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> passwordResetService.resetPassword("   ", "NewPassword123"));

        assertEquals(ApiErrors.RESET_TOKEN_INVALID_OR_EXPIRED, exception.getReason());
        verify(tokenRepository, never()).findByTokenHash(any());
    }
}
