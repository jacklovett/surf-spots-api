package com.lovettj.surfspotsapi.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SkillLevel {
    BEGINNER("Beginner"),
    BEGINNER_INTERMEDIATE("Beginner - Intermediate"),
    INTERMEDIATE("Intermediate"),
    INTERMEDIATE_ADVANCED("Intermediate - Advanced"),
    ADVANCED("Advanced"),
    ALL_LEVELS("All Levels");

    private final String displayName;

    SkillLevel(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
