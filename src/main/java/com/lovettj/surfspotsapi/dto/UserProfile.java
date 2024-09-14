package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.User;

import lombok.Data;

@Data
public class UserProfile {
  private Long id;
  private String username;
  private String name;
  private String email;
  private String country;
  private String region;

  public UserProfile(User user) {
    id = user.getId();
    name = user.getName();
    username = user.getUsername();
    email = user.getEmail();
    country = user.getCountry();
    region = user.getRegion();
  }
}