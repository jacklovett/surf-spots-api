package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegionAndCountryResult {
    private final Region region;
    private final Country country;
}

