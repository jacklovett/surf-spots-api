package com.lovettj.surfspotsapi.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class WatchListDTO {
  private List<NotificationDTO> notifications;
  private List<SurfSpotDTO> surfSpots;
}
