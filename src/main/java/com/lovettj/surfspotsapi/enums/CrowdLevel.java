package com.lovettj.surfspotsapi.enums;

/**
 * Typical crowd on a surf spot, and per surf session log. Stored as enum name in DB.
 */
public enum CrowdLevel {
    EMPTY("Usually quiet"),
    FEW("Usually a small group"),
    BUSY("Usually competitive"),
    PACKED("Usually overcrowded");

    private final String summaryTrendLine;

    CrowdLevel(String summaryTrendLine) {
        this.summaryTrendLine = summaryTrendLine;
    }

    /** Short copy for the spot session summary when this value is the dominant bucket. */
    public String getSummaryTrendLine() {
        return summaryTrendLine;
    }
}
