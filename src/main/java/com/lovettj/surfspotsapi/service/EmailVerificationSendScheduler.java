package com.lovettj.surfspotsapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.repository.UserRepository;

/**
 * Sends verification email off the HTTP request thread so registration stays fast and users can
 * continue onboarding without waiting on SMTP.
 */
@Service
public class EmailVerificationSendScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationSendScheduler.class);

    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;

    public EmailVerificationSendScheduler(
            EmailVerificationService emailVerificationService,
            UserRepository userRepository) {
        this.emailVerificationService = emailVerificationService;
        this.userRepository = userRepository;
    }

    @Async
    public void sendVerificationEmailForUserId(String userId) {
        userRepository
                .findById(userId)
                .ifPresentOrElse(
                        user -> {
                            try {
                                emailVerificationService.sendVerificationEmail(user);
                            } catch (Exception sendException) {
                                logger.warn(
                                        "Async verification email failed for user {}: {}",
                                        userId,
                                        sendException.getMessage(),
                                        sendException);
                            }
                        },
                        () -> logger.warn("Verification email skipped: user id {} not found", userId));
    }
}
