package com.lovettj.surfspotsapi.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BeachBottomType {
  SAND("Sand"),
  REEF("Reef"),
  ROCK("Rock");

  private final String displayName;

  BeachBottomType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }
}
