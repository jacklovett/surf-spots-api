package com.lovettj.surfspotsapi.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AccommodationOption {
    HOTEL("Hotel"),
    HOSTEL("Hostel"),
    CAMPSITE("Campsite"),
    GUESTHOUSE("Guesthouse");

    private final String displayName;

    AccommodationOption(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
