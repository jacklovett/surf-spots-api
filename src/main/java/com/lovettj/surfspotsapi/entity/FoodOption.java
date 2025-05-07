package com.lovettj.surfspotsapi.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FoodOption {
    RESTAURANT("Restaurant"),
    CAFE("Cafe"),
    PUB("Pub"),
    SUPERMARKET("Supermarket");

    private final String displayName;

    FoodOption(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
