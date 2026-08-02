package com.lovettj.surfspotsapi.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class HazardTest {

  @Test
  void testPollutionDisplayNameShouldMatchSheetSeedValue() {
    assertEquals("Pollution", Hazard.POLLUTION.getDisplayName());
  }

  @Test
  void testHazardDisplayNamesShouldIncludeAllSeedValues() {
    Set<String> displayNames =
        Arrays.stream(Hazard.values()).map(Hazard::getDisplayName).collect(Collectors.toSet());
    assertTrue(displayNames.containsAll(
        Set.of(
            "Sharks",
            "Currents",
            "Rips",
            "Rocks",
            "Reef",
            "Crocodiles",
            "Localism",
            "Pollution")));
  }
}
