package com.lovettj.surfspotsapi.requests;

import lombok.Data;

@Data
public class UserRequest {
  private String email;
  private String name;
  private String country;
  private String city;
}
