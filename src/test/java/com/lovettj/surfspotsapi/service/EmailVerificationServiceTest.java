package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.config.AllowedOrigins;
import com.lovettj.surfspotsapi.testutil.AppPropertiesFactory;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.EmailVerificationToken;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.EmailVerificationTokenRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.security.RateLimiter;
import com.lovettj.surfspotsapi.security.TokenHasher;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TripService tripService;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private AllowedOrigins allowedOrigins;

    private EmailVerificationService emailVerificationService;

    private User user;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(
                emailVerificationTokenRepository,
                userRepository,
                emailService,
                tripService,
                rateLimiter,
                allowedOrigins,
                AppPropertiesFactory.localhostDefaults());
        user = new User();
        user.setId("user-1");
        user.setEmail("verify@example.com");
        user.setName("Verify Me");
        user.setEmailVerified(false);
    }

    @Test
    void sendVerificationEmailShouldUsePublicApiPathInVerifyLink() {
        doNothing().when(emailVerificationTokenRepository).deleteByUser(user);
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString(), any());

        emailVerificationService.sendVerificationEmail(user);

        verify(emailService)
                .sendEmail(
                        eq("verify@example.com"),
                        eq("Verify your email for Surf Spots"),
                        eq(TransactionalEmailTemplate.VERIFY_EMAIL.getLogicalName()),
                        argThat(
                                variables -> {
                                    Object link = variables.get("verifyLink");
                                    return link instanceof String
                                            && ((String) link)
                                                    .startsWith(
                                                            "http://localhost:8080/api/auth/verify-email?token=");
                                }));
    }

    @Test
    void verifyEmailWithTokenShouldRejectBlankToken() {
        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> emailVerificationService.verifyEmailWithToken("  "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(ApiErrors.VERIFY_EMAIL_TOKEN_INVALID_OR_EXPIRED, ex.getReason());
    }

    @Test
    void verifyEmailWithTokenShouldMarkUserVerifiedAndProcessTrips() {
        String plaintext = TokenHasher.generateToken();
        String hash = TokenHasher.hash(plaintext);
        EmailVerificationToken token = new EmailVerificationToken(hash, user);
        token.setId(1L);

        when(emailVerificationTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.verifyEmailWithToken(plaintext);

        verify(userRepository).save(any(User.class));
        verify(emailVerificationTokenRepository).delete(token);
        verify(tripService).processPendingInvitations(eq("verify@example.com"), eq("user-1"));
    }

    @Test
    void verifyEmailWithTokenShouldRejectExpiredToken() {
        String plaintext = TokenHasher.generateToken();
        String hash = TokenHasher.hash(plaintext);
        EmailVerificationToken token = new EmailVerificationToken(hash, user);
        token.setId(2L);
        token.setExpiresAt(Instant.now().minusSeconds(60));

        when(emailVerificationTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        assertThrows(
                ResponseStatusException.class,
                () -> emailVerificationService.verifyEmailWithToken(plaintext));
        verify(userRepository, never()).save(any(User.class));
        verify(tripService, never()).processPendingInvitations(anyString(), anyString());
    }
}
