package com.lovettj.surfspotsapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WaveDirection {
    LEFT("Left"),
    RIGHT("Right"),
    LEFT_AND_RIGHT("Left and Right");

    private final String displayName;

    WaveDirection(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}

