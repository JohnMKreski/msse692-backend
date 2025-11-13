package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.response.ApiErrorDto;
import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.dto.response.ProfileResponse;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.service.ProfileService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/profile") // API versioned base path (added v1)
@Tag(name = "Profile", description = "Current user's profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me") // GET /api/v1/profile/me
    @Operation(summary = "Get my profile", description = "Returns the authenticated user's profile or 404 if not found.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        String uid = extractUid(authentication);
        return profileService.getCurrentProfile(uid)
            .<ResponseEntity<?>>map(p -> ResponseEntity.ok(toResponse(p)))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No profile exists for this user"));
    }

    @PostMapping // POST /api/v1/profile
    @Operation(summary = "Upsert my profile", description = "Creates or updates the authenticated user's profile.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<?> upsertMyProfile(Authentication authentication, @Valid @RequestBody ProfileRequest request) {
        String uid = extractUid(authentication);
        boolean existed = profileService.getCurrentProfile(uid).isPresent();
        Profile saved = profileService.upsertProfile(uid, request);
        ProfileResponse body = toResponse(saved);
        if (existed) {
            return ResponseEntity.ok(body);
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
    }

    private String extractUid(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = strClaim(jwt, "sub");
            if (sub != null && !sub.isBlank()) return sub;
            String userId = strClaim(jwt, "user_id");
            if (userId != null && !userId.isBlank()) return userId;
        }
        throw new IllegalStateException("UNAUTHENTICATED");
    }

    private String strClaim(Jwt jwt, String name) {
        Object v = jwt.getClaims().get(name);
        return v != null ? String.valueOf(v) : null;
    }

    private ProfileResponse toResponse(Profile p) {
        ProfileResponse r = new ProfileResponse();
        r.setId(p.getId());
        r.setUserId(p.getUser() != null ? p.getUser().getId() : null);
        r.setDisplayName(p.getDisplayName());
        r.setCompleted(p.isCompleted());
        r.setVerified(p.isVerified());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }

    // Errors are standardized via RestExceptionHandler (ApiErrorDto)
}
