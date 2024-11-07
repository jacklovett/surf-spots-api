package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.User;

import lombok.Data;

@Data
public class UserProfile {
  private Long id;
  private String name;
  private String email;
  private String country;
  private String city;

  public UserProfile(User user) {
    id = user.getId();
    name = user.getName();
    email = user.getEmail();
    country = user.getCountry();
    city = user.getCity();
  }
}