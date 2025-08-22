package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.enums.BeachBottomType;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.SurfSpotType;
import com.lovettj.surfspotsapi.enums.Tide;

import lombok.Data;

import com.lovettj.surfspotsapi.enums.Parking;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;

import java.util.List;

@Data
public class SurfSpotFilterDTO {
    private String userId;
    private List<SurfSpotType> type;
    private List<BeachBottomType> beachBottomType;
    private List<SkillLevel> skillLevel;
    private List<Tide> tide;
    private Double minSurfHeight;
    private Double maxSurfHeight;
    private Integer minRating;
    private Integer maxRating;
    private String swellDirection;
    private String windDirection;
    private String seasonStart;
    private String seasonEnd;
    private List<Parking> parking;
    private SurfSpotStatus status;
    private Boolean boatRequired;
    private List<String> foodOptions;
    private List<String> accommodationOptions;
    private List<String> facilities;
    private List<String> hazards;
    private List<String> forecasts;
}
