package com.lovettj.surfspotsapi.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;

@Service
public class NotificationService {

    private final SwellSeasonService swellSeasonService;
    private final EventNotificationService eventNotificationService;

    public NotificationService(
            SwellSeasonService swellSeasonService,
            EventNotificationService eventNotificationService) {
        this.swellSeasonService = swellSeasonService;
        this.eventNotificationService = eventNotificationService;
    }

    /**
     * Generate notifications for watched surf spots, grouped by region to avoid duplicates
     * 
     * @param watchListSurfSpots List of watched surf spots
     * @return List of notifications
     */
    public List<NotificationDTO> generateNotifications(List<WatchListSurfSpot> watchListSurfSpots) {
        List<NotificationDTO> notifications = new ArrayList<>();

        if (watchListSurfSpots == null || watchListSurfSpots.isEmpty()) {
            return notifications;
        }

        Map<Long, SurfSpot> watchedSpotsById = indexWatchedSpotsById(watchListSurfSpots);
        if (watchedSpotsById.isEmpty()) {
            return notifications;
        }

        notifications.addAll(swellSeasonService.generateSwellSeasonNotifications(watchListSurfSpots));
        notifications.addAll(eventNotificationService.generateEventNotifications(watchedSpotsById));
        
        // Generate promotion notifications (placeholder - would come from external API or database)
        // notifications.addAll(generatePromotionNotifications(watchListSurfSpots));
        
        // Generate hazard notifications (placeholder - would come from external API or database)
        // notifications.addAll(generateHazardNotifications(watchListSurfSpots));

        notifications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return notifications;
    }

    private Map<Long, SurfSpot> indexWatchedSpotsById(List<WatchListSurfSpot> watchListSurfSpots) {
        Map<Long, SurfSpot> watchedSpotsById = new LinkedHashMap<>();
        for (WatchListSurfSpot watchListItem : watchListSurfSpots) {
            SurfSpot surfSpot = watchListItem.getSurfSpot();
            if (surfSpot != null && surfSpot.getId() != null) {
                watchedSpotsById.putIfAbsent(surfSpot.getId(), surfSpot);
            }
        }
        return watchedSpotsById;
    }

}

