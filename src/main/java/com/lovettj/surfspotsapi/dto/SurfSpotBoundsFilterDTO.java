package com.lovettj.surfspotsapi.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class SurfSpotBoundsFilterDTO extends SurfSpotFilterDTO {
    private Double minLatitude;
    private Double maxLatitude;
    private Double minLongitude;
    private Double maxLongitude;
}
