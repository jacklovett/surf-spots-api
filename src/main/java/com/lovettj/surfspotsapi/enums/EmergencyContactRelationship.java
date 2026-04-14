package com.lovettj.surfspotsapi.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How the emergency contact relates to the user. Stored as enum name in the database.
 * JSON uses the display label (same pattern as {@link SkillLevel}).
 */
public enum EmergencyContactRelationship {
    PARENT("Parent"),
    SPOUSE("Spouse"),
    PARTNER("Partner"),
    SIBLING("Sibling"),
    CHILD("Child"),
    FRIEND("Friend"),
    OTHER("Other");

    private final String displayName;

    EmergencyContactRelationship(String displayName) {
        this.displayName = displayName;
    }

    @JsonCreator
    public static EmergencyContactRelationship fromDisplayName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (EmergencyContactRelationship relationship : values()) {
            if (relationship.displayName.equalsIgnoreCase(trimmed)) {
                return relationship;
            }
        }
        throw new IllegalArgumentException("Unknown emergency contact relationship: " + value);
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}
