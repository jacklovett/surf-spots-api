package com.lovettj.surfspotsapi.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.config.AppProperties;
import com.lovettj.surfspotsapi.email.EmailLayoutVariables;
import com.lovettj.surfspotsapi.email.MapboxStaticImageUrls;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.util.SessionTimeZoneUtil;
import com.lovettj.surfspotsapi.util.StringUtils;

/**
 * Sends email notifications for surf sessions.
 */
@Service
public class SessionNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SessionNotificationService.class);

    private static final DateTimeFormatter EMAIL_TIME_FORMAT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy 'at' HH:mm", Locale.ENGLISH);

    private final EmailService emailService;
    private final NearbySurfSpotResolver nearbySurfSpotResolver;
    private final String mapboxAccessToken;
    private final String normalizedAppBaseUrl;

    public SessionNotificationService(
            EmailService emailService,
            NearbySurfSpotResolver nearbySurfSpotResolver,
            AppProperties appProperties) {
        this.emailService = emailService;
        this.nearbySurfSpotResolver = nearbySurfSpotResolver;
        String token = appProperties.getMapbox().getAccessToken();
        this.mapboxAccessToken = token != null && !token.isBlank() ? token : null;
        this.normalizedAppBaseUrl = EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getUrl());
    }

    public void notifySessionStarted(User user, SurfSession session) {
        if (!session.isShareLocationWithEmergencyContact()) {
            return;
        }
        String contactEmail = StringUtils.blankToNull(user.getEmergencyContactEmail());
        if (contactEmail == null) {
            logger.info(
                    "Skipping session-started email for user {}: emergency contact email not set",
                    user.getId());
            return;
        }

        ZoneId zone = SessionTimeZoneUtil.zoneForSession(session);
        Map<String, Object> variables = new HashMap<>();
        variables.put("contactName", contactDisplayName(user));
        variables.put("userName", displayName(user));
        String spotName = resolveSpotNameForSession(session);
        putSpotNameIfPresent(variables, spotName);
        variables.put("startTime", formatInstant(session.getSessionStartInstant(), zone));
        if (session.getExpectedReturnInstant() != null) {
            variables.put("expectedReturnTime", formatInstant(session.getExpectedReturnInstant(), zone));
        }
        variables.put("timeZoneNote", SessionTimeZoneUtil.emailTimeZoneNote(displayName(user), zone));
        applyMapVariables(variables, session.getStartLatitude(), session.getStartLongitude());
        variables.put("appUrl", normalizedAppBaseUrl);

        String subject = startedSubject(displayName(user), spotName);
        emailService.sendEmail(
                contactEmail,
                subject,
                TransactionalEmailTemplate.SESSION_STARTED.getLogicalName(),
                variables);
    }

    public void notifySessionEnded(User user, SurfSession session) {
        if (!session.isShareLocationWithEmergencyContact()) {
            return;
        }
        String contactEmail = StringUtils.blankToNull(user.getEmergencyContactEmail());
        if (contactEmail == null) {
            logger.info(
                    "Skipping session-ended email for user {}: emergency contact email not set",
                    user.getId());
            return;
        }

        ZoneId zone = SessionTimeZoneUtil.zoneForSession(session);
        Map<String, Object> variables = new HashMap<>();
        variables.put("contactName", contactDisplayName(user));
        variables.put("userName", displayName(user));
        putSpotNameIfPresent(variables, resolveSpotNameForSession(session));
        variables.put("startTime", formatInstant(session.getSessionStartInstant(), zone));
        variables.put("endTime", formatInstant(session.getSessionEndInstant(), zone));
        variables.put("duration", formatDuration(session.getDurationMinutes()));
        variables.put("timeZoneNote", SessionTimeZoneUtil.emailTimeZoneNote(displayName(user), zone));
        variables.put("appUrl", normalizedAppBaseUrl);

        String subject = displayName(user) + "'s surf session has ended";
        emailService.sendEmail(
                contactEmail,
                subject,
                TransactionalEmailTemplate.SESSION_ENDED.getLogicalName(),
                variables);
    }

    public void notifySessionOverdue(User user, SurfSession session) {
        if (!session.isShareLocationWithEmergencyContact()) {
            return;
        }
        if (session.getExpectedReturnInstant() == null) {
            return;
        }
        String contactEmail = StringUtils.blankToNull(user.getEmergencyContactEmail());
        if (contactEmail == null) {
            logger.info(
                    "Skipping session-overdue email for user {}: emergency contact email not set",
                    user.getId());
            return;
        }

        ZoneId zone = SessionTimeZoneUtil.zoneForSession(session);
        Map<String, Object> variables = new HashMap<>();
        variables.put("contactName", contactDisplayName(user));
        variables.put("userName", displayName(user));
        putSpotNameIfPresent(variables, resolveSpotNameForSession(session));
        variables.put("startTime", formatInstant(session.getSessionStartInstant(), zone));
        variables.put("expectedReturnTime", formatInstant(session.getExpectedReturnInstant(), zone));
        variables.put("timeZoneNote", SessionTimeZoneUtil.emailTimeZoneNote(displayName(user), zone));
        applyMapVariables(variables, session.getStartLatitude(), session.getStartLongitude());
        variables.put("appUrl", normalizedAppBaseUrl);

        String subject = displayName(user) + " has not ended their surf session yet";
        emailService.sendEmail(
                contactEmail,
                subject,
                TransactionalEmailTemplate.SESSION_OVERDUE.getLogicalName(),
                variables);
    }

    private String resolveSpotNameForSession(SurfSession session) {
        String assignedSpotName = spotNameOrNull(session);
        if (assignedSpotName != null) {
            return assignedSpotName;
        }
        Double latitude = session.getStartLatitude();
        Double longitude = session.getStartLongitude();
        if (latitude == null || longitude == null) {
            return null;
        }
        return nearbySurfSpotResolver
                .findApprovedSpotNameNearCoordinates(latitude, longitude)
                .orElse(null);
    }

    private static void putSpotNameIfPresent(Map<String, Object> variables, String spotName) {
        if (spotName != null) {
            variables.put("spotName", spotName);
        }
    }

    private static String startedSubject(String userName, String spotName) {
        if (spotName != null) {
            return userName + " started a surf session near " + spotName;
        }
        return userName + " started a surf session";
    }

    private void applyMapVariables(Map<String, Object> variables, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return;
        }
        String mapImageUrl =
                MapboxStaticImageUrls.buildStaticMapImageUrl(mapboxAccessToken, latitude, longitude, 500, 250);
        if (mapImageUrl != null) {
            variables.put("mapImageUrl", mapImageUrl);
            variables.put("mapsLink", MapboxStaticImageUrls.buildMapsLink(latitude, longitude));
        }
    }

    private static String spotNameOrNull(SurfSession session) {
        SurfSpot spot = session.getSurfSpot();
        if (spot == null) {
            return null;
        }
        return StringUtils.blankToNull(spot.getName());
    }

    private static String contactDisplayName(User user) {
        String contactName = StringUtils.blankToNull(user.getEmergencyContactName());
        return contactName != null ? contactName : "there";
    }

    private static String displayName(User user) {
        String name = StringUtils.blankToNull(user.getName());
        return name != null ? name : "A user";
    }

    private static String formatInstant(Instant instant, ZoneId zone) {
        if (instant == null) {
            return "";
        }
        return EMAIL_TIME_FORMAT.withZone(zone).format(instant);
    }

    private static String formatDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return "Less than 1 minute";
        }
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        if (hours == 0) {
            return minutes + "m";
        }
        if (minutes == 0) {
            return hours + "h";
        }
        return hours + "h " + minutes + "m";
    }
}
