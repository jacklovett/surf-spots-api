package com.lovettj.surfspotsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class NotificationDTO {
  private String title;
  private String description;
  private String type;
  private String link;
}
