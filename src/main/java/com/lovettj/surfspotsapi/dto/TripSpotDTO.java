package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.TripSpot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSpotDTO {
    private String id;
    private Long surfSpotId;
    private String surfSpotName;
    private Integer surfSpotRating;
    private LocalDateTime addedAt;

    public TripSpotDTO(TripSpot tripSpot) {
        this.id = tripSpot.getId();
        this.surfSpotId = tripSpot.getSurfSpot().getId();
        this.surfSpotName = tripSpot.getSurfSpot().getName();
        this.surfSpotRating = tripSpot.getSurfSpot().getRating();
        this.addedAt = tripSpot.getAddedAt();
    }
}







