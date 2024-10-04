package com.lovettj.surfspotsapi.dto;

import java.util.Optional;
import lombok.Data;

import com.lovettj.surfspotsapi.entity.BeachBottomType;
import com.lovettj.surfspotsapi.entity.SkillLevel;
import com.lovettj.surfspotsapi.entity.SurfSpotType;

@Data
public class SurfSpotFilters {
  private Optional<SurfSpotType> breakType = Optional.empty();
  private Optional<BeachBottomType> beachBottomType = Optional.empty();
  private Optional<Boolean> hasLifeguards = Optional.empty(); // Make amenities object?
  private Optional<SkillLevel> skillLevel = Optional.empty();
}
