package com.lovettj.surfspotsapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Tide {
    ANY("Any"),
    LOW("Low"),
    LOW_MID("Low - Mid"),
    MID("Mid"),
    MID_HIGH("Mid - High"),
    HIGH("High");

    private final String displayName;

    Tide(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
