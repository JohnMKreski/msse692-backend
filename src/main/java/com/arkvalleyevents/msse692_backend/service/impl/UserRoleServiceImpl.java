package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.FirebaseClaimsSyncService;
import com.arkvalleyevents.msse692_backend.service.UserRoleService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    private static final Logger log = LoggerFactory.getLogger(UserRoleServiceImpl.class);

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "EDITOR", "ADMIN");

    private final AppUserRepository appUserRepository;
    private final FirebaseClaimsSyncService claimsSyncService;
    private final UserContextProvider userContextProvider;

    public UserRoleServiceImpl(AppUserRepository appUserRepository,
                               FirebaseClaimsSyncService claimsSyncService,
                               UserContextProvider userContextProvider) {
        this.appUserRepository = appUserRepository;
        this.claimsSyncService = claimsSyncService;
        this.userContextProvider = userContextProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public RolesView getRoles(String uid) {
        AppUser user = getUserOr404(uid);
        return new RolesView(user.getFirebaseUid(), user.getRoles());
    }

    @Override
    @Transactional
    public RolesView addRoles(String uid, Set<String> rolesToAdd) {
        if (rolesToAdd == null || rolesToAdd.isEmpty()) {
            throw new IllegalArgumentException("Body must include roles");
        }
        Set<String> normalized = normalizeRoles(rolesToAdd);
        validateAllowed(normalized);

        AppUser user = getUserOr404(uid);
        Set<String> roles = user.getRoles();
        if (roles == null) roles = new HashSet<>();
        roles.addAll(normalized);
        user.setRoles(roles);
        appUserRepository.save(user);

        Long actorId = userContextProvider.current().userId();
        audit("ADMIN_ADD_ROLES", "actorId", actorId, "targetUid", uid, "added", normalized, "resulting", roles, "outcome", "SUCCESS");

        claimsSyncService.syncUserRolesByUid(user.getFirebaseUid(), true);
        return new RolesView(user.getFirebaseUid(), user.getRoles());
    }

    @Override
    @Transactional
    public RemoveRoleResult removeRole(String uid, String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role path variable is required");
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        validateAllowed(Set.of(normalized));

        AppUser user = getUserOr404(uid);
        boolean removed = user.getRoles() != null && user.getRoles().remove(normalized);
        if (removed) {
            appUserRepository.save(user);
            Long actorId = userContextProvider.current().userId();
            audit("ADMIN_REMOVE_ROLE", "actorId", actorId, "targetUid", uid, "removed", normalized, "resulting", user.getRoles(), "outcome", "SUCCESS");
            claimsSyncService.syncUserRolesByUid(user.getFirebaseUid(), true);
        } else {
            Long actorId = userContextProvider.current().userId();
            audit("ADMIN_REMOVE_ROLE", "actorId", actorId, "targetUid", uid, "removed", normalized, "outcome", "NOT_PRESENT");
        }
        return new RemoveRoleResult(removed, normalized, uid);
    }

    @Override
    @Transactional(readOnly = true)
    public SyncResult syncClaims(String uid, boolean force) {
        AppUser user = getUserOr404(uid);
        Long actorId = userContextProvider.current().userId();
        audit("ADMIN_SYNC_CLAIMS", "actorId", actorId, "targetUid", uid, "force", force, "outcome", "SUCCESS");
        claimsSyncService.syncUserRolesByUid(user.getFirebaseUid(), force);
        return new SyncResult(user.getFirebaseUid(), force);
    }

    private AppUser getUserOr404(String uid) {
        return appUserRepository.findByFirebaseUid(uid)
            .orElseThrow(() -> new EntityNotFoundException("User not found for UID: " + uid));
    }

    private static Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.toUpperCase(Locale.ROOT))
            .collect(Collectors.toSet());
    }

    private static void validateAllowed(Set<String> roles) {
        Set<String> unknown = roles.stream().filter(r -> !ALLOWED_ROLES.contains(r)).collect(Collectors.toSet());
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown roles: " + unknown + ". Allowed: " + ALLOWED_ROLES);
        }
    }

    private void audit(String event, Object... kv) {
        StringBuilder sb = new StringBuilder("audit=true event=").append(event);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object key = kv[i];
            Object val = kv[i + 1];
            if (key == null) continue;
            sb.append(' ').append(key).append('=');
            if (val == null) {
                sb.append("null");
            } else if (val instanceof Iterable<?>) {
                sb.append(toSortedList((Iterable<?>) val));
            } else {
                sb.append(val);
            }
        }
        log.info(sb.toString());
    }

    private java.util.List<String> toSortedList(Iterable<?> it) {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (Object o : it) if (o != null) list.add(o.toString());
        list.sort(java.util.Comparator.naturalOrder());
        return list;
    }
}
