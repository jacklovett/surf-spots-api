package com.lovettj.surfspotsapi.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SkillLevel {
  BEGINNER("Beginner"),
  BEGINNER_INTERMEDIATE("Beginner - Intermediate"),
  INTERMEDIATE("Intermediate"),
  INTERMEDIATE_ADVANCED("Intermediate - Advanced"),
  ADVANCED("Advanced");

  private final String displayName;

  SkillLevel(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }
}
