package com.lovettj.surfspotsapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How the wave face felt on a surf session (surface texture), not overall session quality.
 * Stored as enum name in DB.
 */
public enum WaveFace {
    CLEAN("Clean"),
    MUSHY("Mushy"),
    CHOPPY("Choppy"),
    BLOWN_OUT("Blown out");

    private final String displayName;

    WaveFace(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
