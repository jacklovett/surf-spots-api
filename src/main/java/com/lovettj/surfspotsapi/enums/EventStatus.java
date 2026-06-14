package com.lovettj.surfspotsapi.enums;

import java.util.List;

/** Lifecycle status stored on {@code surf_event.status}. */
public enum EventStatus {
    SCHEDULED,
    UPCOMING,
    ACTIVE,
    COMPLETED,
    CANCELLED;

    /** Statuses that exclude a spot from active-season surf guide and watchlist notifications. */
    public static List<EventStatus> excludedFromSeasonActivity() {
        return List.of(COMPLETED, CANCELLED);
    }

    /** Maps contest schedule page labels onto stored statuses (HTML import adapter only). */
    public static EventStatus fromContestPageLabel(String label) {
        if (label == null || label.isBlank()) {
            return SCHEDULED;
        }
        String normalized = label.trim().toLowerCase();
        return switch (normalized) {
            case "upcoming", "tentative", "standby" -> UPCOMING;
            case "active", "live" -> ACTIVE;
            case "completed" -> COMPLETED;
            case "cancelled", "canceled", "postponed" -> CANCELLED;
            default -> SCHEDULED;
        };
    }
}
