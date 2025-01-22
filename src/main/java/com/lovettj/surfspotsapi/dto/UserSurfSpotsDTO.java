package com.lovettj.surfspotsapi.dto;

import java.util.List;

import com.lovettj.surfspotsapi.entity.BeachBottomType;
import com.lovettj.surfspotsapi.entity.SkillLevel;
import com.lovettj.surfspotsapi.entity.SurfSpotType;

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
  private List<SurfSpotDTO> surfedSpots;
}
