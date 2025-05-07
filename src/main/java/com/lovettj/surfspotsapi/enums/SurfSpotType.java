package com.lovettj.surfspotsapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SurfSpotType {
    BEACH_BREAK("Beach Break"),
    REEF_BREAK("Reef Break"),
    POINT_BREAK("Point Break");

    private final String displayName;

    SurfSpotType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
