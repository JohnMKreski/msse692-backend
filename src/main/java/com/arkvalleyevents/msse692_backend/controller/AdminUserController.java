package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.service.FirebaseClaimsSyncService;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "EDITOR", "ADMIN");

    private final AppUserRepository appUserRepository;
    private final FirebaseClaimsSyncService claimsSyncService;

    public AdminUserController(AppUserRepository appUserRepository, FirebaseClaimsSyncService claimsSyncService) {
        this.appUserRepository = appUserRepository;
        this.claimsSyncService = claimsSyncService;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown roles: " + unknown + ". Allowed: " + ALLOWED_ROLES);
        }
    }

    private AppUser getUserOr404(String uid) {
        return appUserRepository.findByFirebaseUid(uid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for UID: " + uid));
    }

    @GetMapping("/{uid}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public RolesResponse getRoles(@PathVariable String uid) {
        AppUser user = getUserOr404(uid);
        return new RolesResponse(user.getFirebaseUid(), user.getRoles());
    }

    @PostMapping("/{uid}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public RolesResponse addRoles(@PathVariable String uid, @RequestBody RolesRequest body) {
        AppUser user = getUserOr404(uid);
        Set<String> toAdd = normalizeRoles(body != null ? body.roles() : null);
        if (toAdd.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body must include roles");
        }
        validateAllowed(toAdd);
        Set<String> roles = user.getRoles();
        if (roles == null) roles = new HashSet<>();
        roles.addAll(toAdd);
        user.setRoles(roles);
        appUserRepository.save(user);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth != null ? auth.getName() : "unknown");
        log.info("ADMIN addRoles: actor={} uid={} added={} resultingRoles={}", actor, uid, toAdd, roles);
        // Force sync claims after role mutation
        claimsSyncService.syncUserRolesByUid(user.getFirebaseUid(), true);
        return new RolesResponse(user.getFirebaseUid(), user.getRoles());
    }

    @DeleteMapping("/{uid}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeRole(@PathVariable String uid, @PathVariable String role) {
        AppUser user = getUserOr404(uid);
        String normalized = role == null ? null : role.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role path variable is required");
        }
        validateAllowed(Set.of(normalized));
        if (user.getRoles() != null && user.getRoles().remove(normalized)) {
            appUserRepository.save(user);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String actor = (auth != null ? auth.getName() : "unknown");
            log.info("ADMIN removeRole: actor={} uid={} removed={} resultingRoles={}", actor, uid, normalized, user.getRoles());
            claimsSyncService.syncUserRolesByUid(user.getFirebaseUid(), true);
            return ResponseEntity.ok(Map.of("removed", normalized, "uid", uid));
        }
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(Map.of("message", "Role not present", "role", normalized));
    }

    @PostMapping("/{uid}/roles/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncRolesClaims(@PathVariable String uid, @RequestParam(name = "force", defaultValue = "false") boolean force) {
        AppUser user = getUserOr404(uid);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth != null ? auth.getName() : "unknown");
        log.info("ADMIN syncRolesClaims: actor={} uid={} force={}", actor, uid, force);
        claimsSyncService.syncUserRolesByUid(user.getFirebaseUid(), force);
        return ResponseEntity.accepted().body(Map.of(
            "uid", user.getFirebaseUid(),
            "message", "Role claims sync triggered",
            "force", force
        ));
    }
}
