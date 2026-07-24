package com.lovettj.surfspotsapi.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.config.AppProperties;
import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.email.EmailLayoutVariables;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.NotificationEmailSent;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;
import com.lovettj.surfspotsapi.repository.NotificationEmailSentRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;

/**
 * Sends watch-list swell/event emails for users who opted in.
 * Sends first, then claims a dedupe row so failed SMTP can retry and
 * successful SMTP is not re-sent on the next cron run.
 */
@Service
public class WatchListNotificationEmailService {

    private static final Logger logger =
            LoggerFactory.getLogger(WatchListNotificationEmailService.class);

    private final UserRepository userRepository;
    private final WatchListRepository watchListRepository;
    private final NotificationService notificationService;
    private final NotificationEmailSentRepository notificationEmailSentRepository;
    private final EmailService emailService;
    private final String appBaseUrl;

    public WatchListNotificationEmailService(
            UserRepository userRepository,
            WatchListRepository watchListRepository,
            NotificationService notificationService,
            NotificationEmailSentRepository notificationEmailSentRepository,
            EmailService emailService,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.watchListRepository = watchListRepository;
        this.notificationService = notificationService;
        this.notificationEmailSentRepository = notificationEmailSentRepository;
        this.emailService = emailService;
        this.appBaseUrl = EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getUrl());
    }

    public int processWatchListAlertEmails() {
        List<User> users = userRepository.findUsersWithWatchListEmailAlertsEnabled();
        int sentCount = 0;
        for (User user : users) {
            sentCount += processUser(user);
        }
        return sentCount;
    }

    private int processUser(User user) {
        Settings settings = user.getSettings();
        if (settings == null) {
            return 0;
        }

        List<WatchListSurfSpot> watchList = watchListRepository.findByUserId(user.getId());
        if (watchList.isEmpty()) {
            return 0;
        }

        List<NotificationDTO> notifications = notificationService.generateNotifications(watchList);
        int sentCount = 0;
        for (NotificationDTO notification : notifications) {
            if (!shouldEmailNotification(settings, notification)) {
                continue;
            }
            String notificationKey = notification.getId();
            if (notificationKey == null || notificationKey.isBlank()) {
                continue;
            }
            if (sendAndClaim(user, notification, notificationKey)) {
                sentCount++;
            }
        }
        return sentCount;
    }

    private boolean sendAndClaim(
            User user, NotificationDTO notification, String notificationKey) {
        if (notificationEmailSentRepository.existsByUserIdAndNotificationKey(
                user.getId(), notificationKey)) {
            return false;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("alertTitle", notification.getTitle());
        variables.put("alertDescription", notification.getDescription());
        boolean isSwell = "swell".equals(notification.getType());
        variables.put("alertTypeLabel", isSwell ? "Swell season" : "Events and contests");
        if (notification.getLocation() != null && !notification.getLocation().isBlank()) {
            variables.put("alertLocation", notification.getLocation());
        }
        if (notification.getSurfSpotName() != null && !notification.getSurfSpotName().isBlank()) {
            variables.put("alertSpotName", notification.getSurfSpotName());
        }
        if (notification.getStartDate() != null && notification.getEndDate() != null) {
            variables.put(
                    "alertDateRange",
                    notification.getStartDate() + " to " + notification.getEndDate());
        }
        String alertLink = notification.getLink();
        if (alertLink == null || alertLink.isBlank()) {
            alertLink = appBaseUrl + "/watch-list";
        } else if (alertLink.startsWith("/")) {
            alertLink = appBaseUrl + alertLink;
        }
        variables.put("alertLink", alertLink);
        variables.put(
                "alertCtaLabel",
                isSwell
                        ? "View watch list"
                        : (notification.getLink() != null && notification.getLink().startsWith("http")
                                ? "View event details"
                                : "Open in Surf Spots"));
        variables.put("appUrl", appBaseUrl);

        boolean sent =
                emailService.sendEmail(
                        user.getEmail(),
                        notification.getTitle() != null ? notification.getTitle() : "Surf Spots alert",
                        TransactionalEmailTemplate.WATCH_LIST_ALERT.getLogicalName(),
                        variables);
        if (!sent) {
            return false;
        }

        try {
            notificationEmailSentRepository.saveAndFlush(
                    NotificationEmailSent.builder()
                            .user(user)
                            .notificationKey(notificationKey)
                            .sentAt(Instant.now())
                            .build());
            return true;
        } catch (DataIntegrityViolationException duplicateClaim) {
            return false;
        }
    }

    private static boolean shouldEmailNotification(Settings settings, NotificationDTO notification) {
        if (notification == null || notification.getType() == null) {
            return false;
        }
        if ("swell".equals(notification.getType())) {
            return settings.isSwellSeasonEmails();
        }
        if ("event".equals(notification.getType())) {
            return settings.isEventEmails();
        }
        return false;
    }
}
