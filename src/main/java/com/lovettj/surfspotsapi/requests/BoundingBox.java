package com.lovettj.surfspotsapi.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBox {
  private double minLatitude;
  private double maxLatitude;
  private double minLongitude;
  private double maxLongitude;
}
