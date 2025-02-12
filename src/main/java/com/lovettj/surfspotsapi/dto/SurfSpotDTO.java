package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.BeachBottomType;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SkillLevel;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SurfSpotType;

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
    private String swellDirection;
    private String windDirection;
    private String tide;
    private String season;

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
        this.setTide(surfSpot.getTide().toString());
        this.setSeason(surfSpot.getSeason());
    }
}
