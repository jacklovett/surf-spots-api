package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.entity.AuthProvider;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {
  private String email;
  private String name;
  @Enumerated(EnumType.STRING)
  private AuthProvider provider; // Use enum for provider

  private String providerId; // Unique ID from the provider
}
