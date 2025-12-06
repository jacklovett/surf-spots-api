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

    public static WatchListSpotDTO fromWatchListSurfSpot(WatchListSurfSpot watchListSurfSpot, SurfSpotDTO surfSpotDTO) {
        return WatchListSpotDTO.builder()
                .surfSpot(surfSpotDTO)
                .addedAt(watchListSurfSpot.getCreatedAt())
                .build();
    }
}