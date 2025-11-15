package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.response.ApiErrorDto;
import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.FirebaseClaimsSyncService;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin Users", description = "Admin-only user role management")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "EDITOR", "ADMIN");

    private final AppUserRepository appUserRepository;
    private final FirebaseClaimsSyncService claimsSyncService;
    private final UserContextProvider userContextProvider;

    public AdminUserController(AppUserRepository appUserRepository, FirebaseClaimsSyncService claimsSyncService, UserContextProvider userContextProvider) {
        this.appUserRepository = appUserRepository;
        this.claimsSyncService = claimsSyncService;
        this.userContextProvider = userContextProvider;
    }

    public record RolesRequest(Set<String> roles) {}
    public record RolesResponse(String firebaseUid, Set<String> roles) {}

    private static Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.toUpperCase(Locale.ROOT))
            .collect(Collectors.toCollection(HashSet::new));
    }

    private static void validateAllowed(Set<String> roles) {
        Set<String> unknown = roles.stream().filter(r -> !ALLOWED_ROLES.contains(r)).collect(Collectors.toSet());
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown roles: " + unknown + ". Allowed: " + ALLOWED_ROLES);
        }
    }

    private AppUser getUserOr404(String uid) {
        return appUserRepository.findByFirebaseUid(uid)
            .orElseThrow(() -> new EntityNotFoundException("User not found for UID: " + uid));
    }

    @GetMapping("/{uid}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user roles", description = "Returns the roles assigned to the specified user (ADMIN only).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RolesResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public RolesResponse getRoles(@PathVariable String uid) {
        AppUser user = getUserOr404(uid);
        return new RolesResponse(user.getFirebaseUid(), user.getRoles());
    }

    @PostMapping("/{uid}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add roles to user", description = "Adds validated roles to a user and triggers claims sync (ADMIN only).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = RolesResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public RolesResponse addRoles(@PathVariable String uid, @RequestBody RolesRequest body) {
        AppUser user = getUserOr404(uid);
        Set<String> toAdd = normalizeRoles(body != null ? body.roles() : null);
        if (toAdd.isEmpty()) {
            throw new IllegalArgumentException("Body must include roles");
        }
        validateAllowed(toAdd);
        Set<String> roles = user.getRoles();
        if (roles == null) roles = new HashSet<>();
        roles.addAll(toAdd);
        user.setRoles(roles);
        appUserRepository.save(user);
        Long actorId = userContextProvider.current().userId();
        log.info("ADMIN addRoles: actorId={} uid={} added={} resultingRoles={}", actorId, uid, toAdd, roles);
        // Force sync claims after role mutation
        claimsSyncService.syncUserRolesByUid(user.getFirebaseUid(), true);
        return new RolesResponse(user.getFirebaseUid(), user.getRoles());
    }

    @DeleteMapping("/{uid}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove role from user", description = "Removes a role from the user (ADMIN only). Returns 200 or 304 if role absent.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "304", description = "Not Modified"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<?> removeRole(@PathVariable String uid, @PathVariable String role) {
        AppUser user = getUserOr404(uid);
        String normalized = role == null ? null : role.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Role path variable is required");
        }
        validateAllowed(Set.of(normalized));
        if (user.getRoles() != null && user.getRoles().remove(normalized)) {
            appUserRepository.save(user);
            Long actorId = userContextProvider.current().userId();
            log.info("ADMIN removeRole: actorId={} uid={} removed={} resultingRoles={}", actorId, uid, normalized, user.getRoles());
            claimsSyncService.syncUserRolesByUid(user.getFirebaseUid(), true);
            return ResponseEntity.ok(Map.of("removed", normalized, "uid", uid));
        }
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(Map.of("message", "Role not present", "role", normalized));
    }

    @PostMapping("/{uid}/roles/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync user role claims", description = "Triggers Firebase role claims sync for a user (ADMIN only).")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Accepted"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<?> syncRolesClaims(@PathVariable String uid, @RequestParam(name = "force", defaultValue = "false") boolean force) {
        AppUser user = getUserOr404(uid);
        Long actorId = userContextProvider.current().userId();
        log.info("ADMIN syncRolesClaims: actorId={} uid={} force={}", actorId, uid, force);
        claimsSyncService.syncUserRolesByUid(user.getFirebaseUid(), force);
        return ResponseEntity.accepted().body(Map.of(
            "uid", user.getFirebaseUid(),
            "message", "Role claims sync triggered",
            "force", force
        ));
    }
}
