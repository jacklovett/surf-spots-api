package com.lovettj.surfspotsapi.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContinentSummaryDTO {
  private Long id;
  private String name;
  private String slug;
  private String description;
  private List<CountrySummaryDTO> countries;
}
