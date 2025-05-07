package com.lovettj.surfspotsapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Parking {
    FREE("Free"),
    PAID("Paid"),
    STREET("Street"),
    NONE("None");

    private final String displayName;

    Parking(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
