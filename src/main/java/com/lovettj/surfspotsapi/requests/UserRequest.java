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
  
  @Min(50)
  @Max(300)
  private Integer height; // stored in cm
  
  @Min(10)
  @Max(500)
  private Integer weight; // stored in kg
  
  private SkillLevel skillLevel;
}
