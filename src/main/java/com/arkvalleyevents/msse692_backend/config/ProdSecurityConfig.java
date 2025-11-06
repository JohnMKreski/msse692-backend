package com.arkvalleyevents.msse692_backend.config;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

/**
 * Security for non-dev profiles (local/prod):
 * - Validates Firebase JWTs via issuer-uri
 * - Enforces audience match to FIREBASE_PROJECT_ID
 * - Maps roles claim to ROLE_* authorities
 */
@Configuration
@Profile({"local", "prod"})
@EnableMethodSecurity
public class ProdSecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${app.security.firebase.audience}")
    private String audience;

    private final AppUserRepository appUserRepository;
    private final AppUserUpsertFilter appUserUpsertFilter;

    public ProdSecurityConfig(AppUserRepository appUserRepository, AppUserUpsertFilter appUserUpsertFilter) {
        this.appUserRepository = appUserRepository;
        this.appUserUpsertFilter = appUserUpsertFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health", 
                    "/v3/api-docs/**", 
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/enums/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        // Ensure AppUser upsert runs AFTER the JWT is authenticated and SecurityContext is populated
        http.addFilterAfter(appUserUpsertFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            List<String> aud = jwt.getAudience();
            boolean ok = aud != null && aud.contains(audience);
            return ok 
                ? OAuth2TokenValidatorResult.success() 
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        };

        ((org.springframework.security.oauth2.jwt.NimbusJwtDecoder) decoder)
            .setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesClaimConverter());
        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> rolesClaimConverter() {
        return jwt -> {
            Object claim = jwt.getClaims().get("roles");
            List<String> roles = new ArrayList<>();
            if (claim instanceof Collection<?> c) {
                for (Object o : c) {
                    if (o != null) roles.add(String.valueOf(o));
                }
            } else if (claim instanceof String s) {
                roles.add(s);
            }
            if (roles.isEmpty()) {
                // Fallback to DB roles based on Firebase UID (sub/user_id)
                String uid = stringClaim(jwt, "sub");
                if (uid == null || uid.isBlank()) uid = stringClaim(jwt, "user_id");
                if (uid != null && !uid.isBlank()) {
                    Optional<AppUser> u = appUserRepository.findByFirebaseUid(uid);
                    if (u.isPresent()) {
                        Set<String> dbRoles = u.get().getRoles();
                        if (dbRoles != null && !dbRoles.isEmpty()) {
                            roles.addAll(dbRoles);
                        }
                    }
                }
                if (roles.isEmpty()) roles.add("USER");
            }
            return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
        };
    }

    private static String stringClaim(Jwt jwt, String name) {
        Object v = jwt.getClaims().get(name);
        return v != null ? String.valueOf(v) : null;
    }
}
