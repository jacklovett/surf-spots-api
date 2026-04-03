package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.SkillLevel;

import lombok.Data;

@Data
public class UserProfile {
  private String id;
  private String name;
  private String email;
  private String country;
  private String city;
  private Integer age;
  private String gender;
  private Integer height;
  private Integer weight;
  private SkillLevel skillLevel;
  private String emergencyContactName;
  private String emergencyContactPhone;
  private String emergencyContactRelationship;
  private SettingsDTO settings;

  public UserProfile(User user) {
    id = user.getId();
    name = user.getName();
    email = user.getEmail();
    country = user.getCountry();
    city = user.getCity();
    age = user.getAge();
    gender = user.getGender();
    height = user.getHeight();
    weight = user.getWeight();
    skillLevel = user.getSkillLevel();
    emergencyContactName = user.getEmergencyContactName();
    emergencyContactPhone = user.getEmergencyContactPhone();
    emergencyContactRelationship = user.getEmergencyContactRelationship();
    settings = new SettingsDTO(user.getSettings());
  }
}