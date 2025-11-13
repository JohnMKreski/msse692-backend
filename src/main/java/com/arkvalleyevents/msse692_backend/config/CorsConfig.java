package com.arkvalleyevents.msse692_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Spring configuration for global CORS settings.
 * Allows cross-origin requests for all endpoints.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOriginsProperty;

    /**
     * CORS filter with fine-grained control, reading allowed origins from configuration.
     * Supports comma-separated list in app.cors.allowed-origins.
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Parse configured origins (trim and skip blanks). '*' handled explicitly.
        for (String origin : allowedOriginsProperty.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                config.addAllowedOriginPattern(trimmed);
            }
        }
        config.addAllowedMethod(CorsConfiguration.ALL);
        config.addAllowedHeader(CorsConfiguration.ALL);
        // Only allow credentials if not wildcard to avoid browser rejections
        config.setAllowCredentials(!config.getAllowedOriginPatterns().contains("*"));
        config.addExposedHeader(HttpHeaders.LOCATION);
        // Cache preflight for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply to API endpoints only (versioning friendly)
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
