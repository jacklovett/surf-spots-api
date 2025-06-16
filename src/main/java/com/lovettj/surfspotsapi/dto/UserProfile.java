package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.AuthProvider;
import com.lovettj.surfspotsapi.entity.User;

import lombok.Data;

@Data
public class UserProfile {
  private String id;
  private String name;
  private String email;
  private String country;
  private String city;
  private AuthProvider provider;
  private SettingsDTO settings;

  public UserProfile(User user) {
    id = user.getId();
    name = user.getName();
    email = user.getEmail();
    country = user.getCountry();
    city = user.getCity();
    provider = user.getProvider();
    settings = new SettingsDTO(user.getSettings());
  }
}