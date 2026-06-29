package com.lovettj.surfspotsapi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lovettj.surfspotsapi.dto.ContestScheduleImportDTO;

/** Parses CT schedule rows from HTML saved in a browser. Does not fetch URLs. */
public final class ContestScheduleHtmlParser {

    private static final Logger logger = LoggerFactory.getLogger(ContestScheduleHtmlParser.class);
    private static final String CHAMPIONSHIP_TOUR_LABEL = "Championship Tour";

    private static final Pattern EVENT_ROW_PATTERN = Pattern.compile(
            "<tr class=\"event-\\d+[^\"]*\"[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DATE_PATTERN =
            Pattern.compile("<td class=\"event-date-range first\">([^<]+)</td>");
    private static final Pattern LOCATION_PATTERN =
            Pattern.compile("event-schedule-details__location\">([^<]+)</span>");
    private static final Pattern TOUR_PATTERN =
            Pattern.compile("event-tour-details__tour-name\">([^<]+)</span>");
    private static final Pattern STATUS_PATTERN =
            Pattern.compile("class=\"event-status[^\"]*\"[^>]*>\\s*<span>([^<]+)</span>");
    private static final Pattern EVENT_NAME_ANCHOR_OPEN_PATTERN = Pattern.compile(
            "<a\\s[^>]*class=\"[^\"]*event-schedule-details__event-name[^\"]*\"[^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HREF_ATTRIBUTE_PATTERN =
            Pattern.compile("href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_ANCHOR_PATTERN = Pattern.compile(
            "<a\\s[^>]*class=\"[^\"]*event-schedule-details__event-name[^\"]*\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private ContestScheduleHtmlParser() {}

    public static ContestScheduleImportDTO parseScheduleHtml(String html, int year) {
        List<ContestScheduleImportDTO.ContestScheduleEventDTO> events = new ArrayList<>();
        Matcher rowMatcher = EVENT_ROW_PATTERN.matcher(html);

        while (rowMatcher.find()) {
            ContestScheduleImportDTO.ContestScheduleEventDTO eventRow = parseEventRow(rowMatcher.group(1), year);
            if (eventRow != null) {
                events.add(eventRow);
            }
        }

        if (events.isEmpty()) {
            throw new IllegalStateException(
                    "No Championship Tour events found in schedule page for year "
                            + year
                            + ". The page HTML structure may have changed.");
        }

        ContestScheduleImportDTO schedule = new ContestScheduleImportDTO();
        schedule.setYear(year);
        schedule.setEvents(events);
        logger.info("Parsed {} Championship Tour events for year {}", events.size(), year);
        return schedule;
    }

    private static ContestScheduleImportDTO.ContestScheduleEventDTO parseEventRow(String rowHtml, int year) {
        String tourName = firstGroup(TOUR_PATTERN, rowHtml);
        if (tourName == null || !CHAMPIONSHIP_TOUR_LABEL.equalsIgnoreCase(tourName.trim())) {
            return null;
        }

        String dateRangeText = firstGroup(DATE_PATTERN, rowHtml);
        String locationName = firstGroup(LOCATION_PATTERN, rowHtml);
        String eventName = extractEventName(rowHtml);
        String statusLabel = firstGroup(STATUS_PATTERN, rowHtml);

        if (dateRangeText == null || locationName == null || eventName == null) {
            logger.warn("Skipping schedule row with missing date, location, or name");
            return null;
        }

        ContestDateRangeParser.DateRange dateRange = ContestDateRangeParser.parse(dateRangeText, year);

        ContestScheduleImportDTO.ContestScheduleEventDTO eventRow =
                new ContestScheduleImportDTO.ContestScheduleEventDTO();
        eventRow.setName(eventName);
        eventRow.setLocationName(locationName.trim());
        eventRow.setStartDate(dateRange.start());
        eventRow.setEndDate(dateRange.end());
        eventRow.setStatus(statusLabel != null ? statusLabel.trim() : null);
        eventRow.setUrl(extractUrl(rowHtml));
        return eventRow;
    }

    private static String extractUrl(String rowHtml) {
        Matcher anchorMatcher = EVENT_NAME_ANCHOR_OPEN_PATTERN.matcher(rowHtml);
        if (!anchorMatcher.find()) {
            return null;
        }
        Matcher hrefMatcher = HREF_ATTRIBUTE_PATTERN.matcher(anchorMatcher.group(0));
        if (!hrefMatcher.find()) {
            return null;
        }
        return hrefMatcher.group(1).trim();
    }

    private static String extractEventName(String rowHtml) {
        Matcher nameMatcher = NAME_ANCHOR_PATTERN.matcher(rowHtml);
        if (!nameMatcher.find()) {
            return null;
        }
        String anchorInnerHtml = nameMatcher.group(1);
        int sponsorClassIndex = anchorInnerHtml.indexOf("event-schedule-details__sponsor");
        String namePortion = anchorInnerHtml;
        if (sponsorClassIndex >= 0) {
            int sponsorTagStart = anchorInnerHtml.lastIndexOf('<', sponsorClassIndex);
            namePortion = sponsorTagStart >= 0 ? anchorInnerHtml.substring(0, sponsorTagStart) : anchorInnerHtml;
        }
        return HTML_TAG_PATTERN.matcher(namePortion).replaceAll("").trim();
    }

    private static String firstGroup(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
}
