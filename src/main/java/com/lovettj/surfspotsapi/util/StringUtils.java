package com.lovettj.surfspotsapi.util;

/**
 * Shared string helpers for trimming user/API input before persistence or validation.
 */
public final class StringUtils {

    private StringUtils() {}

    /** Returns {@code null} when the value is null, blank, or whitespace-only. */
    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
