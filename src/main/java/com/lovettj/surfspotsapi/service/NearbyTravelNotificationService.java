package com.lovettj.surfspotsapi.service;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.config.AppProperties;
import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.email.EmailLayoutVariables;
import com.lovettj.surfspotsapi.email.MapboxStaticImageUrls;
import com.lovettj.surfspotsapi.email.MapboxStaticImageUrls.MapPin;
import com.lovettj.surfspotsapi.email.TransactionalEmailTemplate;
import com.lovettj.surfspotsapi.entity.NotificationEmailSent;
import com.lovettj.surfspotsapi.entity.Settings;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.repository.NotificationEmailSentRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.util.CoordinateDistanceUtil;
import com.lovettj.surfspotsapi.util.CoordinateDistanceUtil.CoordinateBoundingBox;
import com.lovettj.surfspotsapi.util.DistanceFormatUtil;

/**
 * Web on-visit nearby travel alerts: when nearby-travel emails are enabled and
 * a user's reported location jumps far from their last known position, email a
 * short list of nearby spots. Location is not stored when the preference is off.
 */
@Service
public class NearbyTravelNotificationService {

    private static final Logger logger =
            LoggerFactory.getLogger(NearbyTravelNotificationService.class);

    /** Treat as travel when the user has moved at least this far from last known point. */
    static final double TRAVEL_THRESHOLD_KM = 200.0;

    static final double NEARBY_SPOTS_RADIUS_KM = 50.0;
    static final int NEARBY_SPOTS_LIMIT = 5;

    private final UserRepository userRepository;
    private final SurfSpotRepository surfSpotRepository;
    private final NotificationEmailSentRepository notificationEmailSentRepository;
    private final EmailService emailService;
    private final String appBaseUrl;
    private final String mapboxAccessToken;

    public NearbyTravelNotificationService(
            UserRepository userRepository,
            SurfSpotRepository surfSpotRepository,
            NotificationEmailSentRepository notificationEmailSentRepository,
            EmailService emailService,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.surfSpotRepository = surfSpotRepository;
        this.notificationEmailSentRepository = notificationEmailSentRepository;
        this.emailService = emailService;
        this.appBaseUrl = EmailLayoutVariables.normalizeAppBaseUrl(appProperties.getUrl());
        String token = appProperties.getMapbox() != null ? appProperties.getMapbox().getAccessToken() : null;
        this.mapboxAccessToken = token != null && !token.isBlank() ? token : null;
    }

    @Transactional
    public void reportLocation(String userId, double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Settings settings = user.getSettings();
        if (settings == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User settings not found");
        }

        Double previousLatitude = settings.getLastKnownLatitude();
        Double previousLongitude = settings.getLastKnownLongitude();

        // Nearby-travel location storage is only for that email feature.
        if (!settings.isNearbySurfSpotsEmails() || !user.isEmailVerified()) {
            if (settings.getLastKnownLatitude() != null
                    || settings.getLastKnownLongitude() != null
                    || settings.getLastKnownLocationAt() != null) {
                settings.setLastKnownLatitude(null);
                settings.setLastKnownLongitude(null);
                settings.setLastKnownLocationAt(null);
                userRepository.save(user);
            }
            return;
        }

        settings.setLastKnownLatitude(latitude);
        settings.setLastKnownLongitude(longitude);
        settings.setLastKnownLocationAt(Instant.now());
        userRepository.save(user);

        if (previousLatitude == null || previousLongitude == null) {
            return;
        }

        double travelDistanceKm = CoordinateDistanceUtil.distanceKm(
                previousLatitude, previousLongitude, latitude, longitude);
        if (travelDistanceKm < TRAVEL_THRESHOLD_KM) {
            return;
        }

        String notificationKey =
                "nearby-travel-"
                        + YearMonth.now()
                        + "-"
                        + Math.round(latitude * 10)
                        + "-"
                        + Math.round(longitude * 10);

        if (notificationEmailSentRepository.existsByUserIdAndNotificationKey(
                userId, notificationKey)) {
            return;
        }

        List<NearbySpotEmailItem> nearbySpots =
                findNearbySpots(latitude, longitude, settings.getPreferredUnits());

        if (nearbySpots.isEmpty()) {
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("nearbySpots", nearbySpots);
        variables.put("mapLink", appBaseUrl + "/surf-spots");
        variables.put("appUrl", appBaseUrl);
        String mapImageUrl = buildNearbyMapImageUrl(latitude, longitude, nearbySpots);

        if (mapImageUrl != null) {
            variables.put("mapImageUrl", mapImageUrl);
        }

        boolean sent =
                emailService.sendEmail(
                        user.getEmail(),
                        "Surf spots near your new location",
                        TransactionalEmailTemplate.NEARBY_SURF_SPOTS.getLogicalName(),
                        variables);

        if (!sent) {
            return;
        }

        try {
            notificationEmailSentRepository.save(
                    NotificationEmailSent.builder()
                            .user(user)
                            .notificationKey(notificationKey)
                            .sentAt(Instant.now())
                            .build());
        } catch (DataIntegrityViolationException ignored) {
            // Already claimed by a concurrent request.
            return;
        }

        logger.info(
                "Sent nearby-travel email to userId={} with {} spot(s) after {} km move",
                userId,
                nearbySpots.size(),
                Math.round(travelDistanceKm));
    }

    private List<NearbySpotEmailItem> findNearbySpots(
            double latitude, double longitude, String preferredUnits) {
        CoordinateBoundingBox boundingBox =
                CoordinateDistanceUtil.boundingBoxAroundCoordinates(
                        latitude, longitude, NEARBY_SPOTS_RADIUS_KM);

        SurfSpotBoundsFilterDTO filters = new SurfSpotBoundsFilterDTO();
        filters.setMinLatitude(boundingBox.minLatitude());
        filters.setMaxLatitude(boundingBox.maxLatitude());
        filters.setMinLongitude(boundingBox.minLongitude());
        filters.setMaxLongitude(boundingBox.maxLongitude());
        filters.setStatus(SurfSpotStatus.APPROVED);

        List<SurfSpot> candidates = surfSpotRepository.findWithinBoundsWithFilters(filters);
        List<NearbySpotEmailItem> nearbySpots = new ArrayList<>();

        for (SurfSpot spot : candidates) {
            if (spot.getLatitude() == null || spot.getLongitude() == null || spot.getName() == null) {
                continue;
            }
            
            double distanceKm = CoordinateDistanceUtil.distanceKm(
                    latitude, longitude, spot.getLatitude(), spot.getLongitude());
            
            if (distanceKm > NEARBY_SPOTS_RADIUS_KM) {
                continue;
            }
            
            nearbySpots.add(
                    new NearbySpotEmailItem(
                            spot.getName(),
                            DistanceFormatUtil.formatDistanceKm(distanceKm, preferredUnits),
                            distanceKm,
                            spot.getLatitude(),
                            spot.getLongitude()));
        }

        nearbySpots.sort(Comparator.comparingDouble(NearbySpotEmailItem::sortDistanceKm));
        if (nearbySpots.size() > NEARBY_SPOTS_LIMIT) {
            return nearbySpots.subList(0, NEARBY_SPOTS_LIMIT);
        }
        return nearbySpots;
    }

    private String buildNearbyMapImageUrl(
            double userLatitude, double userLongitude, List<NearbySpotEmailItem> nearbySpots) {
        List<MapPin> spotPins = new ArrayList<>();
        for (NearbySpotEmailItem spot : nearbySpots) {
            if (spot.latitude() == null || spot.longitude() == null) {
                continue;
            }
            spotPins.add(
                    new MapPin(spot.latitude(), spot.longitude(), MapboxStaticImageUrls.PIN_COLOR_SPOT));
        }
        return MapboxStaticImageUrls.buildNearbySpotsMapImageUrl(
                mapboxAccessToken, userLatitude, userLongitude, spotPins, 500, 250);
    }

    /** Thymeleaf-friendly nearby spot row (name + distanceLabel + optional coords for map). */
    public record NearbySpotEmailItem(
            String name,
            String distanceLabel,
            double sortDistanceKm,
            Double latitude,
            Double longitude) {}
}
