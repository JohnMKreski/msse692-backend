package com.arkvalleyevents.msse692_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
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

        /**
         * Global injection of standard error responses (400/401/403/404/409/500) into every operation.
         * Controllers can omit repetitive @ApiResponses; success codes may still be declared locally.
         */
        @Bean
        public OpenApiCustomizer standardErrorResponsesCustomiser() {
                return openApi -> {
                        // Ensure ApiErrorDto schema exists (if class scanned this is optional; define minimal fallback)
                        Components components = openApi.getComponents();
                        if (components.getSchemas() == null || !components.getSchemas().containsKey("ApiErrorDto")) {
                                Schema<?> errSchema = new Schema<>()
                                                .type("object")
                                                .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                                                .addProperty("status", new Schema<>().type("integer"))
                                                .addProperty("error", new Schema<>().type("string"))
                                                .addProperty("message", new Schema<>().type("string"))
                                                .addProperty("path", new Schema<>().type("string"));
                                components.addSchemas("ApiErrorDto", errSchema);
                        }

                        // Build reusable responses
                        ApiResponse badRequest = buildErrorResponse("Bad Request");
                        ApiResponse unauthorized = buildErrorResponse("Unauthorized");
                        ApiResponse forbidden = buildErrorResponse("Forbidden");
                        ApiResponse notFound = buildErrorResponse("Not Found");
                        ApiResponse conflict = buildErrorResponse("Conflict");
                        ApiResponse serverError = buildErrorResponse("Internal Server Error");

                        openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(op -> {
                                // Always include common error responses if absent
                                op.getResponses().putIfAbsent("400", badRequest);
                                op.getResponses().putIfAbsent("401", unauthorized);
                                op.getResponses().putIfAbsent("403", forbidden);
                                op.getResponses().putIfAbsent("404", notFound);
                                // Include 409 for write operations (POST/PUT/PATCH)
                                String verb = op.getOperationId();
                                if (verb != null) {
                                        String vLower = verb.toLowerCase();
                                        if (vLower.startsWith("create") || vLower.startsWith("update") || vLower.startsWith("patch") || vLower.startsWith("upsert") || vLower.startsWith("post") || vLower.startsWith("put")) {
                                                op.getResponses().putIfAbsent("409", conflict);
                                        }
                                }
                                op.getResponses().putIfAbsent("500", serverError);
                        }));
                };
        }

        private ApiResponse buildErrorResponse(String description) {
                return new ApiResponse()
                                .description(description)
                                .content(new Content().addMediaType("application/json",
                                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiErrorDto"))));
        }
}