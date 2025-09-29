package com.arkvalleyevents.msse692_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring configuration for global CORS settings.
 * Allows cross-origin requests for all endpoints.
 */
@Configuration
public class CorsConfig {

    /**
     * Defines a WebMvcConfigurer bean to customize CORS mappings.
     * Applies CORS settings to all request paths.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            /**
             * Configures CORS for all endpoints.
             * - Allows any origin pattern
             * - Permits common HTTP methods
             * - Accepts any headers
             * - Supports credentials (cookies, authorization headers)
             */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*") // Allow requests from any origin
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow specific HTTP methods
                        .allowedHeaders("*") // Allow any headers
                        .allowCredentials(true); // Allow credentials
            }
        };
    }
}
