package com.lovettj.surfspotsapi.dto;

import java.util.List;

import com.lovettj.surfspotsapi.enums.BeachBottomType;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.SurfSpotType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserSurfSpotsDTO {
  private int totalCount;
  private int countryCount;
  private int continentCount;
  private SurfSpotType mostSurfedSpotType;
  private BeachBottomType mostSurfedBeachBottomType;
  private SkillLevel skillLevel;
  private List<SurfedSpotDTO> surfedSpots;
}
