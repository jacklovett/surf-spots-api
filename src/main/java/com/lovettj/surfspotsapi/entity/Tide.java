package com.lovettj.surfspotsapi.entity;

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

    public String getDisplayName() {
        return displayName;
    }
}
