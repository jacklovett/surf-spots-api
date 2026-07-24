package com.lovettj.surfspotsapi.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.config.AppProperties;
import com.lovettj.surfspotsapi.email.EmailLayoutVariables;
import com.lovettj.surfspotsapi.email.MapboxStaticImageUrls;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.NotificationEmailSent;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.repository.NotificationEmailSentRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.util.SurfSpotPathUtil;

/**
 * Emails opted-in members when a surf spot transitions PENDING → APPROVED.
 */
@Service
public class NewSurfSpotEmailService {

    private static final Logger logger = LoggerFactory.getLogger(NewSurfSpotEmailService.class);

    private final UserRepository userRepository;
    private final NotificationEmailSentRepository notificationEmailSentRepository;
    private final EmailService emailService;
    private final String appBaseUrl;
    private final String mapboxAccessToken;

    public NewSurfSpotEmailService(
            UserRepository userRepository,
            NotificationEmailSentRepository notificationEmailSentRepository,
            EmailService emailService,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.notificationEmailSentRepository = notificationEmailSentRepository;
        this.emailService = emailService;
        this.appBaseUrl = EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getUrl());
        String token = appProperties.getMapbox() != null ? appProperties.getMapbox().getAccessToken() : null;
        this.mapboxAccessToken = token != null && !token.isBlank() ? token : null;
    }

    /**
     * Only PENDING → APPROVED triggers mail. Create-as-approved and re-saves of
     * already-approved spots do not. Runs async so spot writes stay fast.
     */
    @Async
    public void notifySubscribersIfApproved(SurfSpot surfSpot, SurfSpotStatus previousStatus) {
        if (surfSpot == null
                || surfSpot.getId() == null
                || surfSpot.getStatus() != SurfSpotStatus.APPROVED
                || previousStatus != SurfSpotStatus.PENDING) {
            return;
        }
        notifySubscribers(surfSpot);
    }

    public void notifySubscribers(SurfSpot surfSpot) {
        String notificationKey = "new-surf-spot-" + surfSpot.getId();
        List<User> recipients = userRepository.findUsersWithNewSurfSpotEmailsEnabled();
        if (recipients.isEmpty()) {
            return;
        }

        String spotName = surfSpot.getName() != null ? surfSpot.getName() : "New surf spot";
        String locationLabel = SurfSpotPathUtil.buildLocationLabel(surfSpot);
        String spotLink = appBaseUrl + SurfSpotPathUtil.pathFor(surfSpot);
        String subject = "New surf spot: " + spotName;
        String mapImageUrl = null;
        
        if (surfSpot.getLatitude() != null && surfSpot.getLongitude() != null) {
            mapImageUrl =
                    MapboxStaticImageUrls.buildStaticMapImageUrl(
                            mapboxAccessToken,
                            surfSpot.getLatitude(),
                            surfSpot.getLongitude(),
                            500,
                            250);
        }

        int sentCount = 0;
        for (User user : recipients) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            String perUserKey = notificationKey + "-" + user.getId();
            if (notificationEmailSentRepository.existsByUserIdAndNotificationKey(
                    user.getId(), perUserKey)) {
                continue;
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("spotName", spotName);
            variables.put("locationLabel", locationLabel);
            variables.put("spotLink", spotLink);
            variables.put("appUrl", appBaseUrl);
            if (mapImageUrl != null) {
                variables.put("mapImageUrl", mapImageUrl);
            }
            boolean sent =
                    emailService.sendEmail(
                            user.getEmail(),
                            subject,
                            TransactionalEmailTemplate.NEW_SURF_SPOT.getLogicalName(),
                            variables);
            if (!sent) {
                continue;
            }
            try {
                notificationEmailSentRepository.saveAndFlush(
                        NotificationEmailSent.builder()
                                .user(user)
                                .notificationKey(perUserKey)
                                .sentAt(Instant.now())
                                .build());
                sentCount++;
            } catch (DataIntegrityViolationException ignored) {
                // Already claimed by another worker.
            }
        }
        logger.info(
                "Sent new-surf-spot email for spot id={} to {} opted-in user(s)",
                surfSpot.getId(),
                sentCount);
    }
}
