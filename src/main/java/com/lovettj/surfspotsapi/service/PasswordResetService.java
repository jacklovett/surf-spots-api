package com.lovettj.surfspotsapi.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.config.AllowedOrigins;
import com.lovettj.surfspotsapi.entity.PasswordResetToken;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.TokenRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.security.RateLimiter;
import com.lovettj.surfspotsapi.security.TokenHasher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final UserService userService;
    private final RateLimiter rateLimiter;
    private final AllowedOrigins allowedOrigins;

    /**
     * Start a password reset flow for the given email address.
     *
     * The response is identical regardless of whether the email exists. We never throw
     * a 404 here because doing so lets an attacker enumerate accounts. Rate limiting,
     * origin validation, and secret generation all happen before the database lookup
     * so timing is roughly constant.
     */
    @Transactional
    public void createPasswordResetToken(String email, String origin, String clientIp) {
        rateLimiter.checkRateLimit(RateLimiter.Bucket.FORGOT_PASSWORD, clientIp);
        rateLimiter.checkRateLimit(RateLimiter.Bucket.FORGOT_PASSWORD, email);

        if (!allowedOrigins.contains(origin)) {
            logger.warn("Password reset blocked: origin not on allowlist ({})", origin);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.INVALID_ORIGIN);
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            logger.info("Password reset requested for unknown email; returning generic response");
            return;
        }
        User user = existing.get();

        String plaintext = TokenHasher.generateToken();
        String hashed = TokenHasher.hash(plaintext);

        tokenRepository.deleteByUser(user);
        tokenRepository.save(new PasswordResetToken(hashed, user));

        sendResetEmail(user, origin, plaintext);
    }

    /**
     * Consume a reset token and set a new password. The lookup is done by hashed
     * token so the database never sees plaintext. A single error message covers
     * unknown, expired, and already-used tokens so attackers cannot probe the
     * validity of arbitrary tokens.
     */
    @Transactional
    public void resetPassword(String token, String password) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.RESET_TOKEN_INVALID_OR_EXPIRED);
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(TokenHasher.hash(token))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, ApiErrors.RESET_TOKEN_INVALID_OR_EXPIRED));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(resetToken);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.RESET_TOKEN_INVALID_OR_EXPIRED);
        }

        userService.setUserPassword(resetToken.getUser(), password);
        tokenRepository.delete(resetToken);
    }

    private void sendResetEmail(User user, String origin, String token) {
        String resetLink = origin + "/reset-password?token=" + token;
        Map<String, Object> emailVariables = new HashMap<>();
        emailVariables.put("resetLink", resetLink);
        emailService.sendEmail(user.getEmail(), "Password Reset Request", "reset-password", emailVariables);
    }
}
