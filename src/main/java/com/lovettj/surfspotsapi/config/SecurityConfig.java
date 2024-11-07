package com.lovettj.surfspotsapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SessionCookieFilter sessionCookieFilter() {
    return new SessionCookieFilter();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // Disable CSRF for API usage
        .authorizeHttpRequests(auth -> auth
                .requestMatchers(SessionCookieFilter.PUBLIC_ENDPOINTS)
            .permitAll() // Public endpoints
            .anyRequest().authenticated() // Protect other endpoints
        )
        .addFilterBefore(sessionCookieFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}