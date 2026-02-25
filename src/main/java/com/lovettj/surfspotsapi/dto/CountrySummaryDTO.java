package com.lovettj.surfspotsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountrySummaryDTO {
  private Long id;
  private String name;
  private String slug;
  private String description;
}
