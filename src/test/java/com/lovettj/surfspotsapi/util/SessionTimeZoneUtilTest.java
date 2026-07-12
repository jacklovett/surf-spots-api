package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.SurfSpot;

class SessionTimeZoneUtilTest {

    @Test
    void zoneFromStartIanaZoneIdShouldReturnUtcWhenMissing() {
        assertEquals(ZoneId.of("UTC"), SessionTimeZoneUtil.zoneFromStartIanaZoneId(null));
        assertEquals(ZoneId.of("UTC"), SessionTimeZoneUtil.zoneFromStartIanaZoneId("  "));
    }

    @Test
    void zoneFromStartIanaZoneIdShouldParseValidZone() {
        assertEquals(
                ZoneId.of("Europe/Dublin"),
                SessionTimeZoneUtil.zoneFromStartIanaZoneId("Europe/Dublin"));
    }

    @Test
    void zoneForSessionShouldPreferSurfSpotZoneOverStartZone() {
        SurfSpot spot = SurfSpot.builder().ianaZoneId("Australia/Sydney").build();
        SurfSession session = SurfSession.builder()
                .surfSpot(spot)
                .startIanaZoneId("Europe/Dublin")
                .build();

        assertEquals(ZoneId.of("Australia/Sydney"), SessionTimeZoneUtil.zoneForSession(session));
    }

    @Test
    void zoneForSessionShouldUseStartZoneWhenNoSpot() {
        SurfSession session = SurfSession.builder()
                .startIanaZoneId("Europe/Dublin")
                .build();

        assertEquals(ZoneId.of("Europe/Dublin"), SessionTimeZoneUtil.zoneForSession(session));
    }

    @Test
    void emailTimeZoneNoteShouldNameSurferAndZone() {
        assertEquals(
                "All times are in Jack's local time (Europe/Dublin).",
                SessionTimeZoneUtil.emailTimeZoneNote("Jack", ZoneId.of("Europe/Dublin")));
    }

    @Test
    void zoneForSessionShouldFormatEuropeDublinOneHourAheadOfUtcInSummer() {
        SurfSession session = SurfSession.builder()
                .startIanaZoneId("Europe/Dublin")
                .sessionStartInstant(Instant.parse("2026-07-09T21:00:00Z"))
                .sessionEndInstant(Instant.parse("2026-07-09T21:05:00Z"))
                .build();

        ZoneId zone = SessionTimeZoneUtil.zoneForSession(session);
        assertEquals(22, session.getSessionStartInstant().atZone(zone).getHour());
        assertEquals(22, session.getSessionEndInstant().atZone(zone).getHour());
    }
}
