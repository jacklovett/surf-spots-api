package com.lovettj.surfspotsapi.util;

import java.time.Month;

/**
 * Utility class for month-related operations
 */
public class MonthUtils {

    /**
     * Parse month string (e.g., "September", "April") to Month enum
     * 
     * @param monthString The month name as a string
     * @return The corresponding Month enum, or null if the string is invalid
     */
    public static Month parseMonthString(String monthString) {
        if (monthString == null || monthString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Capitalize first letter, lowercase rest to match Month enum format
            String normalized = monthString.trim();
            String formatted = normalized.substring(0, 1).toUpperCase() + 
                              normalized.substring(1).toLowerCase();
            return Month.valueOf(formatted.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}



