package com.lovettj.surfspotsapi.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lovettj.surfspotsapi.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.config.AllowedOrigins;
import com.lovettj.surfspotsapi.email.EmailLayoutVariables;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.EmailVerificationToken;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.EmailVerificationTokenRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.security.RateLimiter;
import com.lovettj.surfspotsapi.security.TokenHasher;

@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TripService tripService;
    private final RateLimiter rateLimiter;
    private final AllowedOrigins allowedOrigins;
    private final String normalizedAppBaseUrl;
    private final String normalizedPublicApiBaseUrl;

    public EmailVerificationService(
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            TripService tripService,
            RateLimiter rateLimiter,
            AllowedOrigins allowedOrigins,
            AppProperties appProperties) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.tripService = tripService;
        this.rateLimiter = rateLimiter;
        this.allowedOrigins = allowedOrigins;
        this.normalizedAppBaseUrl = EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getUrl());
        this.normalizedPublicApiBaseUrl =
                EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getPublicApiBaseUrl());
    }

    /**
     * Issues a fresh token and sends the verification email. Deletes any prior tokens for the user.
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        if (user.isEmailVerified()) {
            logger.info("Email already verified for user {}", user.getId());
            return;
        }

        String plaintext = TokenHasher.generateToken();
        String hashed = TokenHasher.hash(plaintext);
        emailVerificationTokenRepository.deleteByUser(user);
        emailVerificationTokenRepository.save(new EmailVerificationToken(hashed, user));

        String verifyLink = normalizedPublicApiBaseUrl + "/api/auth/verify-email?token=" + plaintext;
        Map<String, Object> variables = new HashMap<>();
        variables.put("verifyLink", verifyLink);
        variables.put("appUrl", normalizedAppBaseUrl);
        variables.put("userName", user.getName() != null ? user.getName() : "");
        emailService.sendEmail(
                user.getEmail(),
                "Verify your email for Surf Spots",
                TransactionalEmailTemplate.VERIFY_EMAIL.getLogicalName(),
                variables);
    }

    /**
     * Consumes a verification token, marks the user verified, and processes pending trip invitations.
     */
    @Transactional
    public void verifyEmailWithToken(String plaintextToken) {
        if (plaintextToken == null || plaintextToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ApiErrors.VERIFY_EMAIL_TOKEN_INVALID_OR_EXPIRED);
        }

        Optional<EmailVerificationToken> maybeToken =
                emailVerificationTokenRepository.findByTokenHash(TokenHasher.hash(plaintextToken));
        if (maybeToken.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ApiErrors.VERIFY_EMAIL_TOKEN_INVALID_OR_EXPIRED);
        }

        EmailVerificationToken stored = maybeToken.get();
        if (stored.getExpiresAt().isBefore(java.time.Instant.now())) {
            emailVerificationTokenRepository.delete(stored);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ApiErrors.VERIFY_EMAIL_TOKEN_INVALID_OR_EXPIRED);
        }

        User user = stored.getUser();
        emailVerificationTokenRepository.delete(stored);

        if (user.isEmailVerified()) {
            return;
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        try {
            tripService.processPendingInvitations(user.getEmail(), user.getId());
        } catch (Exception tripException) {
            logger.warn(
                    "Email verified for user {} but pending trip invitations failed: {}",
                    user.getId(),
                    tripException.getMessage(),
                    tripException);
        }
    }

    /**
     * Resend verification for an email if the account exists and is still unverified.
     * Always behaves as success from a caller-observability perspective (no enumeration).
     */
    @Transactional
    public void resendVerificationEmail(String email, String origin, String referer, String clientIp) {
        rateLimiter.checkRateLimit(RateLimiter.Bucket.RESEND_VERIFICATION, clientIp);
        rateLimiter.checkRateLimit(RateLimiter.Bucket.RESEND_VERIFICATION, email);

        if (allowedOrigins.resolveTrustedAppOrigin(origin, referer).isEmpty()) {
            logger.warn("Resend verification blocked: origin/referer not on allowlist (origin={}, referer={})", origin, referer);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.INVALID_ORIGIN);
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            return;
        }
        
        User user = existing.get();
        if (user.isEmailVerified()) {
            return;
        }

        if (user.getPassword() == null) {
            return;
        }

        try {
            sendVerificationEmail(user);
        } catch (Exception sendException) {
            logger.warn("Failed to resend verification email: {}", sendException.getMessage(), sendException);
        }
    }
}
