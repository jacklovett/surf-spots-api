package com.lovettj.surfspotsapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.security.SessionCookieVerifier;

@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** HTTP security only — skipped for {@code event-cli} (no web server). */
    @Configuration
    @EnableWebSecurity
    @Profile("!event-cli")
    static class WebSecurityConfiguration {

        @Bean
        SessionCookieFilter sessionCookieFilter(SessionCookieVerifier sessionCookieVerifier, UserRepository userRepository) {
            return new SessionCookieFilter(sessionCookieVerifier, userRepository);
        }

        @Bean
        CsrfOriginFilter csrfOriginFilter(AllowedOrigins allowedOrigins, ObjectMapper objectMapper) {
            return new CsrfOriginFilter(allowedOrigins, objectMapper);
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                SessionCookieFilter sessionCookieFilter,
                CsrfOriginFilter csrfOriginFilter) throws Exception {
            http
                    .cors(Customizer.withDefaults())
                    // Spring synchronizer-token CSRF is disabled for cross-origin JSON clients.
                    // CsrfOriginFilter enforces Origin/Referer against the same allowlist as CORS
                    // (OWASP defence in depth with SameSite=Lax on the session cookie).
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/continents/**").permitAll()
                    .requestMatchers("/api/countries/**").permitAll()
                    .requestMatchers("/api/regions/**").permitAll()
                    .requestMatchers("/api/surf-spots/region-id/**").permitAll()
                    .requestMatchers("/api/surf-spots/sub-region/**").permitAll()
                    .requestMatchers("/api/surf-spots/within-bounds").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/surf-spots/management").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/surf-spots/management/*").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/surf-spots/management/*").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/surf-spots/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/surf-spots/id/*").permitAll()
                    // Dev-only HTML email previews (bean exists only with profile "dev")
                    .requestMatchers("/api/dev/mail-preview/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated()
                    )
                    .addFilterBefore(csrfOriginFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(sessionCookieFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }
    }
}
