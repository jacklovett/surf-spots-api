package com.lovettj.surfspotsapi.entity;

public enum SurfSpotType {
  BEACH_BREAK("Beach Break"),
  REEF_BREAK("Reef Break"),
  POINT_BREAK("Point Break");

  private final String displayName;

  SurfSpotType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
