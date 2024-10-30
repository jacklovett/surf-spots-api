package com.lovettj.surfspotsapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // Disable CSRF for API usage
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/user/login", "/api/user/register",
                "/api/user/profile", "/api/continents/**",
                "/api/countries/**", "/api/regions/**",
                "api/surf-spots/**")
            .permitAll() // Public
            // endpoints
            .anyRequest().authenticated() // Protect other endpoints
        );

    return http.build();
  }
}
