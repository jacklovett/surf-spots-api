package com.lovettj.surfspotsapi.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class NotificationDTO {
  private String id;
  private String title;
  private String description;
  private String type;
  private String link;
  private String imageUrl;
  private String location;
  private String surfSpotName;
  private LocalDateTime createdAt;
}
