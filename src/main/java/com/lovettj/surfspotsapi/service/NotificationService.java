package com.lovettj.surfspotsapi.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;

@Service
public class NotificationService {

    private final SwellSeasonService swellSeasonService;

    public NotificationService(SwellSeasonService swellSeasonService) {
        this.swellSeasonService = swellSeasonService;
    }

    /**
     * Generate notifications for watched surf spots, grouped by region to avoid duplicates
     * 
     * @param watchListSurfSpots List of watched surf spots
     * @return List of notifications
     */
    public List<NotificationDTO> generateNotifications(List<WatchListSurfSpot> watchListSurfSpots) {
        List<NotificationDTO> notifications = new ArrayList<>();

        if (watchListSurfSpots.isEmpty()) {
            return notifications;
        }

        // Generate swell season notifications
        notifications.addAll(swellSeasonService.generateSwellSeasonNotifications(watchListSurfSpots));

        // Generate event notifications (placeholder - would come from external API or database)
        // notifications.addAll(generateEventNotifications(watchListSurfSpots));
        
        // Generate promotion notifications (placeholder - would come from external API or database)
        // notifications.addAll(generatePromotionNotifications(watchListSurfSpots));
        
        // Generate hazard notifications (placeholder - would come from external API or database)
        // notifications.addAll(generateHazardNotifications(watchListSurfSpots));

        // Sort by createdAt, newest first
        notifications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return notifications;
    }

}

