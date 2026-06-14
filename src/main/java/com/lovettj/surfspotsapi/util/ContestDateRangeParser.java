package com.lovettj.surfspotsapi.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses contest schedule date ranges such as {@code Apr 1 - 11} or {@code May 30 - Jun 1}.
 */
public final class ContestDateRangeParser {

    private static final Pattern CROSS_MONTH_PATTERN =
            Pattern.compile("^(\\w{3})\\s+(\\d{1,2})\\s+-\\s+(\\w{3})\\s+(\\d{1,2})$");
    private static final Pattern SAME_MONTH_PATTERN = Pattern.compile("^(\\w{3})\\s+(\\d{1,2})\\s+-\\s+(\\d{1,2})$");
    private static final DateTimeFormatter MONTH_DAY_YEAR =
            DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH);

    private ContestDateRangeParser() {}

    public record DateRange(LocalDate start, LocalDate end) {}

    public static DateRange parse(String dateRangeText, int year) {
        if (dateRangeText == null || dateRangeText.isBlank()) {
            throw new IllegalArgumentException("Contest date range is blank");
        }

        String trimmedRange = dateRangeText.trim();
        Matcher crossMonthMatcher = CROSS_MONTH_PATTERN.matcher(trimmedRange);
        if (crossMonthMatcher.matches()) {
            LocalDate startDate = parseMonthDay(crossMonthMatcher.group(1), crossMonthMatcher.group(2), year);
            int endYear = year;
            if (monthOrder(crossMonthMatcher.group(3)) < startDate.getMonthValue()) {
                endYear = year + 1;
            }
            LocalDate endDate = parseMonthDay(crossMonthMatcher.group(3), crossMonthMatcher.group(4), endYear);
            return new DateRange(startDate, endDate);
        }

        Matcher sameMonthMatcher = SAME_MONTH_PATTERN.matcher(trimmedRange);
        if (sameMonthMatcher.matches()) {
            LocalDate startDate = parseMonthDay(sameMonthMatcher.group(1), sameMonthMatcher.group(2), year);
            LocalDate endDate = parseMonthDay(sameMonthMatcher.group(1), sameMonthMatcher.group(3), year);
            return new DateRange(startDate, endDate);
        }

        throw new IllegalArgumentException("Unrecognized contest date range: " + dateRangeText);
    }

    private static LocalDate parseMonthDay(String monthAbbreviation, String dayText, int year) {
        try {
            return LocalDate.parse(monthAbbreviation + " " + dayText + " " + year, MONTH_DAY_YEAR);
        } catch (DateTimeParseException parseException) {
            throw new IllegalArgumentException(
                    "Invalid date in contest range: " + monthAbbreviation + " " + dayText, parseException);
        }
    }

    private static int monthOrder(String monthAbbreviation) {
        return LocalDate.parse(monthAbbreviation + " 1 2000", MONTH_DAY_YEAR).getMonthValue();
    }
}
