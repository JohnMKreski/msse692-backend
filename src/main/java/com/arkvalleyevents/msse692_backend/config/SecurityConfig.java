package com.arkvalleyevents.msse692_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Development-only security: open endpoints for fast iteration against H2/dev.
 */
@Configuration
@Profile("dev-disabled")
public class SecurityConfig {
    @Bean(name = "devOpenSecurityFilterChain")
    public SecurityFilterChain devOpenSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
