package com.arkvalleyevents.msse692_backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Lightweight projection of an AppUser with roles for admin listing & detail endpoints.
 * This decouples internal persistence model from external representation and allows
 * future soft-delete or additional flags without breaking clients.
 */
@Schema(name = "AppUserWithRoles", description = "Admin view of an application user including roles")
public record AppUserWithRolesDto(
        @Schema(description = "Internal numeric identifier", example = "42") Long id,
        @Schema(description = "Firebase UID", example = "abc123XYZ") String firebaseUid,
        @Schema(description = "Email address", example = "user@example.com") String email,
        @Schema(description = "Display name", example = "Jane Doe") String displayName,
        @Schema(description = "Photo URL", example = "https://example.com/avatar.jpg") String photoUrl,
        @Schema(description = "Creation timestamp (UTC ISO-8601)", example = "2025-11-20T18:42:10Z") OffsetDateTime createdAt,
        @Schema(description = "Last update timestamp (UTC ISO-8601)", example = "2025-11-20T19:03:55Z") OffsetDateTime updatedAt,
        @Schema(description = "Assigned roles", example = "[ADMIN,EDITOR]") Set<String> roles
) {}
