package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.dto.response.ProfileResponse;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.service.mapping.ProfileMapper;
import com.arkvalleyevents.msse692_backend.service.ProfileService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
 

@RestController
@RequestMapping("/api/v1/profile") // API versioned base path (added v1)
@Tag(name = "Profile", description = "Current user's profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileMapper profileMapper;

    public ProfileController(ProfileService profileService, ProfileMapper profileMapper) {
        this.profileService = profileService;
        this.profileMapper = profileMapper;
    }

    @GetMapping("/me") // GET /api/v1/profile/me
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my profile", description = "Returns the authenticated user's profile.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    })
    public ResponseEntity<?> getMyProfile() {
        return profileService.getCurrentProfile()
            .<ResponseEntity<?>>map(p -> ResponseEntity.ok(profileMapper.toResponse(p)))
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Profile not found"));
    }

    // Deprecated combined upsert (retained temporarily); prefer explicit CRUD below
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upsert my profile (deprecated)", description = "Creates or updates the authenticated user's profile. Prefer create/update endpoints.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    })
    public ResponseEntity<?> upsertMyProfile(@Valid @RequestBody ProfileRequest request) {
        boolean existed = profileService.getCurrentProfile().isPresent();
        Profile saved = profileService.upsertCurrentProfile(request);
        ProfileResponse body = profileMapper.toResponse(saved);
        if (existed) {
            return ResponseEntity.ok(body);
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
    }

    // ---- Explicit CRUD endpoints ----

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create my profile", description = "Creates a profile for the authenticated user; fails if one exists.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    })
    public ResponseEntity<ProfileResponse> createMyProfile(@Valid @RequestBody ProfileRequest request) {
        Profile created = profileService.createCurrentProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profileMapper.toResponse(created));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update my profile (full)", description = "Full replacement of mutable profile fields.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    })
    public ResponseEntity<ProfileResponse> updateMyProfile(@Valid @RequestBody ProfileRequest request) {
        Profile updated = profileService.updateCurrentProfile(request);
        return ResponseEntity.ok(profileMapper.toResponse(updated));
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Patch my profile (partial)", description = "Partial update; null fields are ignored.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    })
    public ResponseEntity<ProfileResponse> patchMyProfile(@RequestBody ProfileRequest request) {
        Profile patched = profileService.patchCurrentProfile(request);
        return ResponseEntity.ok(profileMapper.toResponse(patched));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete my profile", description = "Deletes the authenticated user's profile.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "No Content")
    })
    public ResponseEntity<Void> deleteMyProfile() {
        profileService.deleteCurrentProfile();
        return ResponseEntity.noContent().build();
    }

    // ---- Admin endpoints (manage any user's profile) ----
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get user profile", description = "Fetch profile by userId.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    })
    public ResponseEntity<ProfileResponse> getProfileByUserId(@PathVariable Long userId) {
        Profile p = profileService.getProfileByUserId(userId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Profile not found"));
        return ResponseEntity.ok(profileMapper.toResponse(p));
    }

    @PostMapping("/{userId}/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Create user profile", description = "Create profile for userId; fails if exists.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    })
    public ResponseEntity<ProfileResponse> createProfileForUser(@PathVariable Long userId, @Valid @RequestBody ProfileRequest request) {
        Profile created = profileService.createProfileForUser(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profileMapper.toResponse(created));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Update user profile (full)", description = "Full replacement for userId.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    })
    public ResponseEntity<ProfileResponse> updateProfileForUser(@PathVariable Long userId, @Valid @RequestBody ProfileRequest request) {
        Profile updated = profileService.updateProfileForUser(userId, request);
        return ResponseEntity.ok(profileMapper.toResponse(updated));
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Patch user profile", description = "Partial update for userId.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    })
    public ResponseEntity<ProfileResponse> patchProfileForUser(@PathVariable Long userId, @RequestBody ProfileRequest request) {
        Profile patched = profileService.patchProfileForUser(userId, request);
        return ResponseEntity.ok(profileMapper.toResponse(patched));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Delete user profile", description = "Delete profile for userId.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "No Content")
    })
    public ResponseEntity<Void> deleteProfileForUser(@PathVariable Long userId) {
        profileService.deleteProfileForUser(userId);
        return ResponseEntity.noContent().build();
    }

    // Errors are standardized via RestExceptionHandler (ApiErrorDto)
}
