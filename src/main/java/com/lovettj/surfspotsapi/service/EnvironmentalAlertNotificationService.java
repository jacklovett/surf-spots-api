package com.lovettj.surfspotsapi.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.EnvironmentalAlert;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertStatus;
import com.lovettj.surfspotsapi.repository.EnvironmentalAlertRepository;

@Service
public class EnvironmentalAlertNotificationService {

    private final EnvironmentalAlertRepository environmentalAlertRepository;

    public EnvironmentalAlertNotificationService(
            EnvironmentalAlertRepository environmentalAlertRepository) {
        this.environmentalAlertRepository = environmentalAlertRepository;
    }

    /**
     * Uses {@code watchedSpotsById} for spot names so callers without an open persistence
     * context (e.g. email cron) do not trigger lazy loads on {@code alert.surfSpot}.
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> generateHazardNotifications(Map<Long, SurfSpot> watchedSpotsById) {
        if (watchedSpotsById == null || watchedSpotsById.isEmpty()) {
            return List.of();
        }

        List<NotificationDTO> notifications = new ArrayList<>();
        List<EnvironmentalAlert> activeAlerts =
                environmentalAlertRepository.findBySurfSpotIdInAndStatusOrderByDetectedAtDesc(
                        watchedSpotsById.keySet(), EnvironmentalAlertStatus.ACTIVE);

        for (EnvironmentalAlert alert : activeAlerts) {
            Long spotId = alert.getSurfSpot() != null ? alert.getSurfSpot().getId() : null;
            SurfSpot watchedSpot = spotId != null ? watchedSpotsById.get(spotId) : null;
            String spotName = watchedSpot != null ? watchedSpot.getName() : null;
            notifications.add(NotificationDTO.builder()
                    .id("environmental-alert-" + alert.getId())
                    .type("hazard")
                    .title(alert.getTitle())
                    .description(alert.getDescription())
                    .link(alert.getSourceUrl())
                    .location(alert.getSourceName())
                    .surfSpotName(spotName)
                    .status(alert.getSeverity() != null ? alert.getSeverity().name() : null)
                    .createdAt(toLocalDateTime(alert.getDetectedAt()))
                    .build());
        }
        return notifications;
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
