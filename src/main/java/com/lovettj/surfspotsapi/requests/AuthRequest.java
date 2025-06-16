package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.entity.AuthProvider;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class AuthRequest {
  private String email;
  private String password;
  private String name;
  @Enumerated(EnumType.STRING)
  private AuthProvider provider;
  private String providerId;
}
