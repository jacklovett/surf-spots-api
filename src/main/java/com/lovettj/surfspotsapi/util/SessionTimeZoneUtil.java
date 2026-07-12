package com.lovettj.surfspotsapi.util;

import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.SurfSpot;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Resolves the IANA zone used to derive local clock labels from UTC instants on a session.
 */
public final class SessionTimeZoneUtil {

    private SessionTimeZoneUtil() {}

    public static ZoneId zoneForSession(SurfSession session) {
        if (session == null) {
            return ZoneId.of("UTC");
        }
        SurfSpot spot = session.getSurfSpot();
        if (spot != null) {
            return zoneForSpot(spot);
        }
        return zoneFromStartIanaZoneId(session.getStartIanaZoneId());
    }

    public static ZoneId zoneForSpot(SurfSpot surfSpot) {
        if (surfSpot == null) {
            return ZoneId.of("UTC");
        }
        String raw = surfSpot.getIanaZoneId();
        if (raw != null && !raw.isBlank()) {
            try {
                return ZoneId.of(raw.trim());
            } catch (DateTimeException exception) {
                return ZoneId.of("UTC");
            }
        }
        return ZoneId.of("UTC");
    }

    public static ZoneId zoneFromStartIanaZoneId(String startIanaZoneId) {
        if (startIanaZoneId == null || startIanaZoneId.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(startIanaZoneId.trim());
        } catch (DateTimeException exception) {
            return ZoneId.of("UTC");
        }
    }

    /** User-facing copy for session safety emails (clock times at the surfer's location). */
    public static String emailTimeZoneNote(String surferName, ZoneId zone) {
        return "All times are in " + surferName + "'s local time (" + zone.getId() + ").";
    }
}
