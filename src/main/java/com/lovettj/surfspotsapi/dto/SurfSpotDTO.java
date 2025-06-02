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
import com.lovettj.surfspotsapi.enums.BeachBottomType;
import com.lovettj.surfspotsapi.enums.Parking;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.SurfSpotType;
import com.lovettj.surfspotsapi.enums.Tide;

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
    private String season;
    private Double minSurfHeight;
    private Double maxSurfHeight;

    private Parking parking;
    private Boolean boatRequired;

    private Boolean foodNearby;
    private List<FoodOption> foodOptions;
    private Boolean accommodationNearby;
    private List<AccommodationOption> accommodationOptions;
    private List<Facility> facilities;
    private List<Hazard> hazards;
    private List<String> forecasts;

    public SurfSpotDTO(SurfSpot surfSpot) {
        this.setId(surfSpot.getId());
        this.setName(surfSpot.getName());
        this.setDescription(surfSpot.getDescription());
        this.setType(surfSpot.getType());
        this.setSkillLevel(surfSpot.getSkillLevel());
        this.setBeachBottomType(surfSpot.getBeachBottomType());
        this.setLatitude(surfSpot.getLatitude());
        this.setLongitude(surfSpot.getLongitude());

        Region surfSpotRegion = surfSpot.getRegion();
        Country surfSpotCountry = (surfSpotRegion != null) ? surfSpotRegion.getCountry() : null;
        Continent surfSpotContinent = (surfSpotCountry != null) ? surfSpotCountry.getContinent() : null;

        this.setRegion(surfSpotRegion);
        this.setCountry(surfSpotCountry);
        this.setContinent(surfSpotContinent);

        this.setSwellDirection(surfSpot.getSwellDirection());
        this.setWindDirection(surfSpot.getWindDirection());
        this.setTide(surfSpot.getTide());
        this.setSeason(surfSpot.getSeasonStart() + " - " + surfSpot.getSeasonEnd());
        this.setRating(surfSpot.getRating());
        this.setMinSurfHeight(surfSpot.getMinSurfHeight());
        this.setMaxSurfHeight(surfSpot.getMaxSurfHeight());
        
        this.setParking(surfSpot.getParking());
        this.setBoatRequired(surfSpot.getBoatRequired());
        this.setFoodNearby(surfSpot.getFoodNearby());
        this.setFoodOptions(surfSpot.getFoodOptions());
        this.setAccommodationNearby(surfSpot.getAccommodationNearby());
        this.setAccommodationOptions(surfSpot.getAccommodationOptions());
        this.setFacilities(surfSpot.getFacilities());
        this.setHazards(surfSpot.getHazards());
        this.setForecasts(surfSpot.getForecasts());
    }
}
