package com.arkvalleyevents.msse692_backend.service;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.impl.UserRoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private FirebaseClaimsSyncService firebaseClaimsSyncService;
    @Mock
    private UserContextProvider userContextProvider;

    @InjectMocks
    private UserRoleServiceImpl service;

    private AppUser user;

    @BeforeEach
    void setup() {
        // lenient because some tests throw before audit uses current()
        lenient().when(userContextProvider.current()).thenReturn(new UserContext(42L, true, true));
        user = new AppUser();
        user.setId(1L);
        user.setFirebaseUid("uid-xyz");
        // use mutable set since service mutates roles
        user.setRoles(new java.util.HashSet<>(Set.of("USER")));
        // lenient because validation failure tests won't hit repository
        lenient().when(appUserRepository.findByFirebaseUid("uid-xyz")).thenReturn(Optional.of(user));
        lenient().when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addRoles_successAddsAndSyncs() {
        var result = service.addRoles("uid-xyz", Set.of("editor"));
        assertTrue(result.roles().contains("EDITOR"));
        verify(appUserRepository).save(any(AppUser.class));
        verify(firebaseClaimsSyncService).syncUserRolesByUid("uid-xyz", true);
    }

    @Test
    void addRoles_unknownRoleThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.addRoles("uid-xyz", Set.of("INVALID")));
        assertTrue(ex.getMessage().contains("Unknown roles"));
    }

    @Test
    void removeRole_successRemovesAndSyncs() {
        // Ensure role present first
        service.addRoles("uid-xyz", Set.of("EDITOR"));
        reset(firebaseClaimsSyncService); // isolate remove call sync
        var result = service.removeRole("uid-xyz", "editor");
        assertTrue(result.removed());
        verify(appUserRepository, atLeastOnce()).save(any(AppUser.class));
        verify(firebaseClaimsSyncService).syncUserRolesByUid("uid-xyz", true);
    }

    @Test
    void removeRole_notPresentReturnsNotModified() {
        var result = service.removeRole("uid-xyz", "EDITOR"); // not present initially
        assertFalse(result.removed());
        verify(appUserRepository, never()).save(any());
        verify(firebaseClaimsSyncService, never()).syncUserRolesByUid(any(), anyBoolean());
    }

    @Test
    void syncClaims_callsFirebase() {
        service.syncClaims("uid-xyz", true);
        verify(firebaseClaimsSyncService).syncUserRolesByUid("uid-xyz", true);
    }
}
