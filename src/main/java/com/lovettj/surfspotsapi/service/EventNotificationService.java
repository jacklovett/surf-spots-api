package com.lovettj.surfspotsapi.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.SurfEvent;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EventStatus;
import com.lovettj.surfspotsapi.enums.EventType;
import com.lovettj.surfspotsapi.repository.SurfEventRepository;

@Service
public class EventNotificationService {

    private final SurfEventRepository surfEventRepository;

    public EventNotificationService(SurfEventRepository surfEventRepository) {
        this.surfEventRepository = surfEventRepository;
    }

    public List<NotificationDTO> generateEventNotifications(Map<Long, SurfSpot> watchedSpotsById) {
        List<NotificationDTO> notifications = new ArrayList<>();

        if (watchedSpotsById.isEmpty()) {
            return notifications;
        }

        int currentYear = LocalDate.now().getYear();
        List<SurfEvent> activeEvents = surfEventRepository.findSeasonActiveEventsForYearAndSurfSpotIds(
                EventType.CONTEST,
                currentYear,
                watchedSpotsById.keySet(),
                EventStatus.excludedFromSeasonActivity());

        LocalDate today = LocalDate.now();
        Set<Long> notifiedEventIds = new HashSet<>();

        for (SurfEvent event : activeEvents) {
            if (event.getSurfSpot() == null || event.getId() == null) {
                continue;
            }
            if (notifiedEventIds.contains(event.getId())) {
                continue;
            }
            if (today.isBefore(event.getStartDate()) || today.isAfter(event.getEndDate())) {
                continue;
            }

            SurfSpot linkedSpot = watchedSpotsById.get(event.getSurfSpot().getId());
            if (linkedSpot == null) {
                continue;
            }

            notifiedEventIds.add(event.getId());

            String description = String.format(
                    "%s is on the WSL CT at %s through %s.",
                    event.getName(),
                    event.getLocationName(),
                    event.getEndDate());

            String title = event.getStatus() == EventStatus.ACTIVE
                    ? event.getName() + " — CT stop live"
                    : event.getName() + " — CT waiting period open";

            NotificationDTO notification = NotificationDTO.builder()
                    .id("surf-event-" + event.getId())
                    .type("event")
                    .title(title)
                    .description(description)
                    .location(event.getLocationName())
                    .surfSpotName(linkedSpot.getName())
                    .createdAt(LocalDateTime.now())
                    .build();

            notifications.add(notification);
        }

        return notifications;
    }
}
