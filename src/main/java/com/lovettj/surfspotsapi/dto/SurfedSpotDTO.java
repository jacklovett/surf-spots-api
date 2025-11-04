package com.lovettj.surfspotsapi.dto;

import java.time.LocalDateTime;

import com.lovettj.surfspotsapi.entity.UserSurfSpot;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SurfedSpotDTO {
    private SurfSpotDTO surfSpot;
    private LocalDateTime addedAt;
    private boolean isFavourite;

    public static SurfedSpotDTO fromUserSurfSpot(UserSurfSpot userSurfSpot) {
        // SurfSpotDTO constructor now sets the path automatically
        return SurfedSpotDTO.builder()
                .surfSpot(new SurfSpotDTO(userSurfSpot.getSurfSpot()))
                .addedAt(userSurfSpot.getCreatedAt())
                .isFavourite(userSurfSpot.isFavourite())
                .build();
    }
}