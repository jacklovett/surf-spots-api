package com.lovettj.surfspotsapi.requests;

import java.util.List;

import com.lovettj.surfspotsapi.entity.AccommodationOption;
import com.lovettj.surfspotsapi.entity.Facility;
import com.lovettj.surfspotsapi.entity.FoodOption;
import com.lovettj.surfspotsapi.entity.Hazard;
import com.lovettj.surfspotsapi.enums.BeachBottomType;
import com.lovettj.surfspotsapi.enums.Parking;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.enums.SurfSpotType;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveDirection;

import lombok.Data;

@Data
public class SurfSpotRequest {

    private String name;
    private String description;
    private Long regionId;
    private Double latitude;
    private Double longitude;
    private SurfSpotType type;
    private BeachBottomType beachBottomType;
    private SkillLevel skillLevel;
    private Tide tide;
    private WaveDirection waveDirection;
    private Parking parking;
    private String swellDirection;
    private String windDirection;
    private Double minSurfHeight;
    private Double maxSurfHeight;
    private String seasonStart;
    private String seasonEnd;
    private Integer rating;
    private boolean boatRequired;
    private boolean isWavepool;
    private String wavepoolUrl;
    private boolean isRiverWave;
    private boolean accommodationNearby;
    private boolean foodNearby;
    private List<Hazard> hazards;
    private List<FoodOption> foodOptions;
    private List<Facility> facilities;
    private List<AccommodationOption> accommodationOptions;
    private List<String> forecasts;
    private SurfSpotStatus status;
    private String userId;
}
