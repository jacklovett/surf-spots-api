package com.lovettj.surfspotsapi.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Facility {
    WC("WC"),
    SHOWERS("Showers"),
    RENTALS("Rentals"),
    SURF_SHOP("Surf Shop"),
    SURF_LESSONS("Surf Lessons");

    private final String displayName;

    Facility(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
