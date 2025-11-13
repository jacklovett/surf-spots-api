package com.lovettj.surfspotsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionLookupRequest {
    private Double longitude;
    private Double latitude;
    private String countryName;
}

