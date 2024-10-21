package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.BeachBottomType;
import com.lovettj.surfspotsapi.entity.SkillLevel;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SurfSpotType;

import lombok.Data;

@Data
public class SurfSpotDTO {
  private Long id;
  private String slug;
  private String path;
  private String name;
  private String description;
  private SurfSpotType type;
  private SkillLevel skillLevel;
  private BeachBottomType beachBottomType;
  private Double latitude;
  private Double longitude;
  private Boolean isSurfedSpot;
  private Boolean isWatched;

  public SurfSpotDTO(SurfSpot surfSpot) {
    this.setId(surfSpot.getId());
    this.setName(surfSpot.getName());
    this.setDescription(surfSpot.getDescription());
    this.setType(surfSpot.getType());
    this.setSkillLevel(surfSpot.getSkillLevel());
    this.setBeachBottomType(surfSpot.getBeachBottomType());
    this.setLatitude(surfSpot.getLatitude());
    this.setLongitude(surfSpot.getLongitude());
  }
}