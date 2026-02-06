package com.lovettj.surfspotsapi.dto;

import java.util.List;

import com.lovettj.surfspotsapi.entity.AccommodationOption;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Facility;
import com.lovettj.surfspotsapi.entity.FoodOption;
import com.lovettj.surfspotsapi.entity.Hazard;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SwellSeason;
import com.lovettj.surfspotsapi.enums.BeachBottomType;
import com.lovettj.surfspotsapi.enums.Parking;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.enums.SurfSpotType;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveDirection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SurfSpotDTO {

    private Long id;
    private String slug;
    private String path;
    private String name;
    private String description;
    private String createdBy;
    private SurfSpotType type;
    private SkillLevel skillLevel;
    private BeachBottomType beachBottomType;
    private Double latitude;
    private Double longitude;
    private Boolean isSurfedSpot;
    private Boolean isWatched;

    private Continent continent;
    private Country country;
    private Region region;

    private Integer rating;
    private String swellDirection;
    private String windDirection;
    private Tide tide;
    private WaveDirection waveDirection;
    private SwellSeason swellSeason;
    private Double minSurfHeight;
    private Double maxSurfHeight;

    private Parking parking;
    private Boolean boatRequired;
    private Boolean isWavepool;
    private String wavepoolUrl;
    private Boolean isRiverWave;
    private SurfSpotStatus status;

    private Boolean foodNearby;
    private List<FoodOption> foodOptions;
    private Boolean accommodationNearby;
    private List<AccommodationOption> accommodationOptions;
    private List<Facility> facilities;
    private List<Hazard> hazards;
    private List<String> forecasts;

    public SurfSpotDTO(SurfSpot surfSpot) {
        this.setId(surfSpot.getId());
        this.setSlug(surfSpot.getSlug());
        this.setName(surfSpot.getName());
        this.setDescription(surfSpot.getDescription());
        this.setCreatedBy(surfSpot.getCreatedBy());
        this.setType(surfSpot.getType());
        this.setSkillLevel(surfSpot.getSkillLevel());
        this.setBeachBottomType(surfSpot.getBeachBottomType());
        this.setLatitude(surfSpot.getLatitude());
        this.setLongitude(surfSpot.getLongitude());

        Region surfSpotRegion = surfSpot.getRegion();
        if (surfSpotRegion != null) {
            Country surfSpotCountry = surfSpotRegion.getCountry();
            Continent surfSpotContinent = surfSpotCountry != null ? surfSpotCountry.getContinent() : null;

            this.setRegion(surfSpotRegion);
            this.setCountry(surfSpotCountry);
            this.setContinent(surfSpotContinent);
        }

        this.setSwellDirection(surfSpot.getSwellDirection());
        this.setWindDirection(surfSpot.getWindDirection());
        this.setTide(surfSpot.getTide());
        this.setWaveDirection(surfSpot.getWaveDirection());
        this.setSwellSeason(surfSpot.getSwellSeason());
        this.setRating(surfSpot.getRating());
        this.setMinSurfHeight(surfSpot.getMinSurfHeight());
        this.setMaxSurfHeight(surfSpot.getMaxSurfHeight());
        
        this.setParking(surfSpot.getParking());
        this.setBoatRequired(surfSpot.getBoatRequired());
        this.setIsWavepool(surfSpot.getIsWavepool());
        this.setWavepoolUrl(surfSpot.getWavepoolUrl());
        this.setIsRiverWave(surfSpot.getIsRiverWave());
        this.setStatus(surfSpot.getStatus());
        this.setFoodNearby(surfSpot.getFoodNearby());
        this.setFoodOptions(surfSpot.getFoodOptions());
        this.setAccommodationNearby(surfSpot.getAccommodationNearby());
        this.setAccommodationOptions(surfSpot.getAccommodationOptions());
        this.setFacilities(surfSpot.getFacilities());
        this.setHazards(surfSpot.getHazards());
        this.setForecasts(surfSpot.getForecasts());
        
        // Generate and set the path for the surf spot
        this.setPath(generateSurfSpotPath(surfSpot));
    }
    
    private String generateSurfSpotPath(SurfSpot surfSpot) {
        Region region = surfSpot.getRegion();
        Country country = region != null ? region.getCountry() : null;
        Continent continent = country != null ? country.getContinent() : null;

        if (continent == null || country == null || region == null) {
            return "";
        }

        if (surfSpot.getSubRegion() != null) {
            return String.format("/surf-spots/%s/%s/%s/sub-regions/%s/%s",
                    continent.getSlug(),
                    country.getSlug(),
                    region.getSlug(),
                    surfSpot.getSubRegion().getSlug(),
                    surfSpot.getSlug());
        } else {
            return String.format("/surf-spots/%s/%s/%s/%s",
                    continent.getSlug(),
                    country.getSlug(),
                    region.getSlug(),
                    surfSpot.getSlug());
        }
    }
}
