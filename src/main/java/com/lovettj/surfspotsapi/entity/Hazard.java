package com.lovettj.surfspotsapi.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Hazard {
    SHARKS("Sharks"),
    CURRENTS("Currents"),
    RIPS("Rips"),
    ROCKS("Rocks"),
    REEF("Reef"),
    CROCODILES("Crocodiles"),
    LOCALISM("Localism");

    private final String displayName;

    Hazard(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
