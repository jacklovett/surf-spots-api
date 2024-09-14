package com.lovettj.surfspotsapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.lang.NonNull;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry.addMapping("/**") // This allows CORS for all endpoints
        .allowedOrigins("http://localhost:3000") // React app's address
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed HTTP methods
        .allowedHeaders("*") // Allow any headers
        .allowCredentials(true); // Allow credentials like cookies
  }
}