package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.enums.SkillLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UserRequest {
  private String email;
  private String name;
  private String country;
  private String city;
  
  @Min(13)
  @Max(120)
  private Integer age;
  
  private String gender;
  
  @Min(50) // 50 cm (metric min) or 20 inches = 51 cm (imperial min, but metric min is lower)
  @Max(305) // 120 inches = 305 cm (imperial max after conversion)
  private Integer height; // stored in cm
  
  @Min(9) // 20 lbs = 9 kg (imperial min after conversion)
  @Max(500) // 500 kg (metric max)
  private Integer weight; // stored in kg
  
  private SkillLevel skillLevel;
}
