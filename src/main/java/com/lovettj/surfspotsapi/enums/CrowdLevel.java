package com.lovettj.surfspotsapi.enums;

/**
 * Surf session log only. Stored as enum name in DB.
 */
public enum CrowdLevel {
    EMPTY("Usually empty lineups"),
    FEW("Usually few people out"),
    BUSY("Often busy"),
    PACKED("Often packed");

    private final String summaryTrendLine;

    CrowdLevel(String summaryTrendLine) {
        this.summaryTrendLine = summaryTrendLine;
    }

    /** Short copy for the spot session summary when this value is the dominant bucket. */
    public String getSummaryTrendLine() {
        return summaryTrendLine;
    }
}
