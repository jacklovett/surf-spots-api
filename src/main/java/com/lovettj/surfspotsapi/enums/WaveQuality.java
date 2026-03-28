package com.lovettj.surfspotsapi.enums;

/**
 * Surf session log only. Stored as enum name in DB.
 */
public enum WaveQuality {
    POOR("Mostly poor waves"),
    OKAY("Mostly okay waves"),
    FUN("Mostly fun waves"),
    GREAT("Mostly great waves");

    private final String summaryTrendLine;

    WaveQuality(String summaryTrendLine) {
        this.summaryTrendLine = summaryTrendLine;
    }

    /** Short copy for the spot session summary when this value is the dominant bucket. */
    public String getSummaryTrendLine() {
        return summaryTrendLine;
    }
}
