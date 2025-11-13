package com.lovettj.surfspotsapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Enable CORS using the CorsConfig bean
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API usage
                .authorizeHttpRequests(auth -> auth
                // Explicitly allow all public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/continents/**").permitAll()
                .requestMatchers("/api/countries/**").permitAll()
                .requestMatchers("/api/regions/**").permitAll() // Explicitly include this
                .requestMatchers("/api/surf-spots/region/**").permitAll()
                .requestMatchers("/api/surf-spots/sub-region/**").permitAll()
                .requestMatchers("/api/surf-spots/within-bounds").permitAll()
                .requestMatchers("/api/surf-spots/*").permitAll() // GET by slug
                .requestMatchers("/api/surf-spots/id/*").permitAll() // GET by id
                .requestMatchers("/error").permitAll() // Allow error endpoint for public requests
                .anyRequest().authenticated() // Protect other endpoints
                )
                .addFilterBefore(sessionCookieFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
