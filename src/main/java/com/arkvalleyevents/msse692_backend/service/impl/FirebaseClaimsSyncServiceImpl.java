package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.service.FirebaseClaimsSyncService;
import com.google.firebase.auth.FirebaseAuth;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FirebaseClaimsSyncServiceImpl implements FirebaseClaimsSyncService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseClaimsSyncServiceImpl.class);

    private final AppUserRepository appUserRepository;
    private final FirebaseAuth firebaseAuth;

    public FirebaseClaimsSyncServiceImpl(AppUserRepository appUserRepository, FirebaseAuth firebaseAuth) {
        this.appUserRepository = appUserRepository;
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    @Transactional(readOnly = true)
    public void syncUserRolesByUid(String firebaseUid, boolean force) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            log.warn("Refusing to sync roles: firebaseUid is null/blank");
            return;
        }
        Optional<AppUser> opt = appUserRepository.findByFirebaseUid(firebaseUid);
        if (opt.isEmpty()) {
            log.warn("Cannot sync claims: AppUser not found for UID {}", firebaseUid);
            return;
        }
        AppUser user = opt.get();
        Set<String> roles = user.getRoles() == null ? Set.of() : user.getRoles();
        if (log.isDebugEnabled()) {
            log.debug("Preparing claim sync: uid={}, rawRoles={} (size={})", firebaseUid, roles, roles.size());
        }
        // Ensure deterministic ordering & uppercase
        List<String> normalized = roles.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(r -> r.toUpperCase(Locale.ROOT))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        // Always ensure at least USER for consistency (optional business rule)
        if (normalized.isEmpty()) {
            normalized = new ArrayList<>();
            normalized.add("USER");
        } else if (!normalized.contains("USER")) {
            normalized.add(0, "USER");
        }

        String rolesHash = hash(normalized);
        if (log.isDebugEnabled()) {
            log.debug("Normalized roles for uid={} => {} (size={}), hash={}", firebaseUid, normalized, normalized.size(), rolesHash);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", normalized);
        claims.put("roles_version", rolesHash); // Helpful for future drift detection

        long t0 = System.nanoTime();
        try {
            // NOTE: Admin SDK does not provide existing custom claims directly for arbitrary user without extra fetch;
            // for now we always push unless future optimization stores last pushed hash in DB.
            log.info("Syncing roles to Firebase claims: uid={} roles={} hash={} force={}", firebaseUid, normalized, rolesHash, force);
            firebaseAuth.setCustomUserClaims(firebaseUid, claims);
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            log.info("Synced Firebase claims successfully: uid={} roles={} hash={} durationMs={}", firebaseUid, normalized, rolesHash, ms);
        } catch (Exception e) {
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            log.error("Failed to set custom claims: uid={} roles={} hash={} durationMs={} error={}",
                firebaseUid, normalized, rolesHash, ms, e.getMessage(), e);
        }
    }

    private String hash(List<String> roles) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(String.join("|", roles).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "na"; // Should never happen for SHA-256
        }
    }
}
