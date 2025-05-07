package com.lovettj.surfspotsapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SurfSpotStatus {
    APPROVED("Approved"),
    PENDING("Pending"),
    PRIVATE("Private");

    private final String displayName;

    SurfSpotStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
