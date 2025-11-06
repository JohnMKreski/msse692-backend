package com.arkvalleyevents.msse692_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring configuration for OpenAPI (Swagger) documentation.
 * Sets up API info, server URLs, and security scheme for Ark Valley Events API.
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    /**
     * Defines the OpenAPI bean for Swagger UI.
     * - Sets API title, version, and description
     * - Configures server URLs (local and optional public)
     * - Adds API key (Firebase JWT) security scheme in Authorization header
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Ark Valley Events API")
                        .version("1.0")
                        .description("Backend REST API for Ark Valley Events"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server")
                        // add prod server later, e.g. new Server().url("https://api.arkvalleyevents.com")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }

    /**
     * Redirects the root URL ("/") to the Swagger UI page.
     * Makes Swagger UI accessible at the application root.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui/index.html");
    }
}