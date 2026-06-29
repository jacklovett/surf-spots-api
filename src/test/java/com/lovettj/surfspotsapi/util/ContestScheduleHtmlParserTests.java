package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.lovettj.surfspotsapi.dto.ContestScheduleImportDTO;

class ContestScheduleHtmlParserTests {

    @Test
    void testParseScheduleHtmlShouldExtractChampionshipTourEventsFromFixture() throws Exception {
        Path fixturePath = Path.of("src/test/resources/contest-schedule-ct-2026-sample.html");
        String html = Files.readString(fixturePath);

        ContestScheduleImportDTO schedule = ContestScheduleHtmlParser.parseScheduleHtml(html, 2026);

        assertEquals(2026, schedule.getYear());
        assertEquals(12, schedule.getEvents().size());

        ContestScheduleImportDTO.ContestScheduleEventDTO rioPro = schedule.getEvents().stream()
                .filter(eventRow -> eventRow.getLocationName().contains("Saquarema"))
                .findFirst()
                .orElseThrow();

        assertEquals("VIVO Rio Pro", rioPro.getName());
        assertEquals("Saquarema, Rio de Janeiro, Brazil", rioPro.getLocationName());
        assertEquals(LocalDate.of(2026, 6, 19), rioPro.getStartDate());
        assertEquals(LocalDate.of(2026, 6, 27), rioPro.getEndDate());
        assertEquals("Upcoming", rioPro.getStatus());
        assertEquals(
                "https://www.worldsurfleague.com/events/2026/ct/440/vivo-rio-pro/main",
                rioPro.getUrl());

        ContestScheduleImportDTO.ContestScheduleEventDTO elSalvadorPro = schedule.getEvents().stream()
                .filter(eventRow -> eventRow.getLocationName().contains("Punta Roca"))
                .findFirst()
                .orElseThrow();

        assertEquals("Surf City El Salvador Pro", elSalvadorPro.getName());
        assertEquals("Live", elSalvadorPro.getStatus());
    }

    @Test
    void testParseScheduleHtmlShouldRejectEmptyScheduleTable() {
        IllegalStateException thrownException = assertThrows(
                IllegalStateException.class,
                () -> ContestScheduleHtmlParser.parseScheduleHtml(
                        "<html><body><table class=\"tableType-event\"></table></body></html>", 2026));

        assertTrue(thrownException.getMessage().contains("No Championship Tour events found"));
    }
}
