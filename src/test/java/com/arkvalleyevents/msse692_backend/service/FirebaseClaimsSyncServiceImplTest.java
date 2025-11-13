package com.arkvalleyevents.msse692_backend.service;

import static org.mockito.Mockito.*;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.service.impl.FirebaseClaimsSyncServiceImpl;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for FirebaseClaimsSyncServiceImpl focusing on claim map composition
 * and interaction with FirebaseAuth. External network calls are mocked.
 */
class FirebaseClaimsSyncServiceImplTest {

    private AppUserRepository appUserRepository;
    private FirebaseAuth firebaseAuth;
    private FirebaseClaimsSyncServiceImpl service;

    @BeforeEach
    void setup() {
        appUserRepository = mock(AppUserRepository.class);
        firebaseAuth = mock(FirebaseAuth.class);
        service = new FirebaseClaimsSyncServiceImpl(appUserRepository, firebaseAuth);
    }

    @Test
    @DisplayName("Adds USER when roles empty and pushes claims")
    void syncAddsUserWhenEmpty() throws FirebaseAuthException {
        AppUser u = new AppUser();
        u.setFirebaseUid("uid123");
        u.setRoles(new HashSet<>()); // empty
        when(appUserRepository.findByFirebaseUid("uid123")).thenReturn(Optional.of(u));

        service.syncUserRolesByUid("uid123", true);

        ArgumentCaptor<String> uidCap = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings({"rawtypes","unchecked"})
    ArgumentCaptor<Map<String,Object>> claimsCap = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    verify(firebaseAuth, times(1)).setCustomUserClaims(uidCap.capture(), claimsCap.capture());

        assert uidCap.getValue().equals("uid123");
        @SuppressWarnings("unchecked")
        java.util.List<String> roles = (java.util.List<String>) claimsCap.getValue().get("roles");
        assert roles.contains("USER") : "USER should be injected when empty";
        assert claimsCap.getValue().get("roles_version") != null : "roles_version hash present";
    }

    @Test
    @DisplayName("Normalizes and hashes roles, ensures USER present")
    void syncNormalizesRoles() throws FirebaseAuthException {
        AppUser u = new AppUser();
        u.setFirebaseUid("uidABC");
        Set<String> r = new HashSet<>();
        r.add("admin");
        r.add("editor");
        u.setRoles(r);
        when(appUserRepository.findByFirebaseUid("uidABC")).thenReturn(Optional.of(u));

        service.syncUserRolesByUid("uidABC", true);

    @SuppressWarnings({"rawtypes","unchecked"})
    ArgumentCaptor<Map<String,Object>> claimsCap = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    verify(firebaseAuth).setCustomUserClaims(eq("uidABC"), claimsCap.capture());
    @SuppressWarnings("unchecked")
    java.util.List<String> roles = (java.util.List<String>) claimsCap.getValue().get("roles");
        assert roles.stream().allMatch(rn -> rn.equals(rn.toUpperCase())) : "Roles must be uppercased";
        assert roles.contains("USER") : "USER enforced for consistency";
        assert roles.contains("ADMIN") && roles.contains("EDITOR") : "Original roles normalized";
    }

    @Test
    @DisplayName("No action when user not found")
    void syncSkipsMissingUser() {
        when(appUserRepository.findByFirebaseUid("missing")).thenReturn(Optional.empty());
        service.syncUserRolesByUid("missing", true);
        verifyNoInteractions(firebaseAuth);
    }

    @Test
    @DisplayName("Handles general exception from FirebaseAuth without propagating")
    void syncHandlesFirebaseError() throws FirebaseAuthException {
        AppUser u = new AppUser();
        u.setFirebaseUid("uidErr");
        u.setRoles(Set.of("ADMIN"));
        when(appUserRepository.findByFirebaseUid("uidErr")).thenReturn(Optional.of(u));
        doThrow(new RuntimeException("Simulated failure"))
            .when(firebaseAuth).setCustomUserClaims(anyString(), any());
        service.syncUserRolesByUid("uidErr", true);
        verify(firebaseAuth).setCustomUserClaims(eq("uidErr"), any());
        // No assertion on logs; absence of thrown exception is success criteria.
    }
}
