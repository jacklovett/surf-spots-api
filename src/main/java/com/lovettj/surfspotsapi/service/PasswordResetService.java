package com.lovettj.surfspotsapi.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.entity.PasswordResetToken;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.TokenRepository;
import com.lovettj.surfspotsapi.security.RateLimiter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final UserService userService;
    private final RateLimiter rateLimiter;

    // TODO: Update allowed origins
    private final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "https://surfspots.com",
            "https://stg-surfspots.com",
            "http://localhost:5173"
    );

    @Transactional
    public void createPasswordResetToken(String email, String origin) {
        rateLimiter.checkRateLimit(email);

        if (!ALLOWED_ORIGINS.contains(origin)) {
            logger.warn("Invalid origin attempt from: {}", origin);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid origin");
        }

        User user = userService.findUserByEmail(email);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        // Ensure only one active token per user
        tokenRepository.deleteByUser(user);
        tokenRepository.save(resetToken);

        sendResetEmail(user, origin, token);
    }

    @Transactional
    public void resetPassword(String token, String password) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token has expired");
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
