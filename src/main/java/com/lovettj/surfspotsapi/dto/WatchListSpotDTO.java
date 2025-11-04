package com.lovettj.surfspotsapi.dto;

import java.time.LocalDateTime;

import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WatchListSpotDTO {
    private SurfSpotDTO surfSpot;
    private LocalDateTime addedAt;

    public static WatchListSpotDTO fromWatchListSurfSpot(WatchListSurfSpot watchListSurfSpot) {
        // SurfSpotDTO constructor now sets the path automatically
        return WatchListSpotDTO.builder()
                .surfSpot(new SurfSpotDTO(watchListSurfSpot.getSurfSpot()))
                .addedAt(watchListSurfSpot.getCreatedAt())
                .build();
    }
}