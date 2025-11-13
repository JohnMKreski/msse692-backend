package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.response.ApiErrorDto;
import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/app-users") // API versioned base path (added v1)
@Tag(name = "AppUsers", description = "AppUser utilities for current user")
public class AppUserController {

    private final AppUserRepository repository;

    public AppUserController(AppUserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me") // GET /api/v1/app-users/me
    @Operation(summary = "Get current AppUser", description = "Returns the AppUser record for the authenticated Firebase UID, or 404 if not found.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = AppUser.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<?> me(Authentication authentication) {
        String uid = extractUid(authentication);
        Optional<AppUser> user = repository.findByFirebaseUid(uid);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No app user exists for this token.");
    }

    private String extractUid(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            Object sub = jwt.getClaims().get("sub");
            if (sub != null && !String.valueOf(sub).isBlank()) return String.valueOf(sub);
            Object userId = jwt.getClaims().get("user_id");
            if (userId != null && !String.valueOf(userId).isBlank()) return String.valueOf(userId);
        }
        throw new IllegalStateException("UNAUTHENTICATED");
    }

    // Standardized errors are handled by RestExceptionHandler using ApiErrorDto
}
