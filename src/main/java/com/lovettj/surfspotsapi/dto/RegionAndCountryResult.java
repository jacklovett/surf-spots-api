package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Region;

import lombok.Data;

@Data
public class RegionAndCountryResult {
    private final Region region;
    private final Country country;
    private final Continent continent;
    
    public RegionAndCountryResult(Region region, Country country) {
        this.region = region;
        this.country = country;
        this.continent = country.getContinent();
    }
}

