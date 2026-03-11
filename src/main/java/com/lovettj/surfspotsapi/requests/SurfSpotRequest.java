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
import com.lovettj.surfspotsapi.validators.ValidHttpUrl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class SurfSpotRequest {

    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_WAVEPOOL_URL_LENGTH = 500;
    private static final int MAX_FORECASTS = 3;
    private static final int MAX_WEBCAMS = 3;

    @NotBlank(message = "Name is required")
    @Size(max = MAX_NAME_LENGTH, message = "Name must be at most " + MAX_NAME_LENGTH + " characters")
    private String name;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters")
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

    @ValidHttpUrl
    @Size(max = MAX_WAVEPOOL_URL_LENGTH, message = "Wavepool URL must be at most " + MAX_WAVEPOOL_URL_LENGTH + " characters")
    private String wavepoolUrl;
    private boolean isRiverWave;
    private boolean accommodationNearby;
    private boolean foodNearby;
    private List<Hazard> hazards;
    private List<FoodOption> foodOptions;
    private List<Facility> facilities;
    private List<AccommodationOption> accommodationOptions;

    @Size(max = MAX_FORECASTS, message = "At most " + MAX_FORECASTS + " forecast links are allowed")
    private List<String> forecasts;

    @Size(max = MAX_WEBCAMS, message = "At most " + MAX_WEBCAMS + " webcam links are allowed")
    private List<String> webcams;
    private SurfSpotStatus status;
    private String userId;
}
