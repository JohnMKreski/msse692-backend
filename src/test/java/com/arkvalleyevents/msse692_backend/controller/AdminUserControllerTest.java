package com.arkvalleyevents.msse692_backend.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.FirebaseClaimsSyncService;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc
@SuppressWarnings("null")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private ProfileRepository profileRepository; // required by AppUserUpsertFilter

    @MockitoBean
    private FirebaseClaimsSyncService claimsSyncService;

    @MockitoBean
    private UserContextProvider userContextProvider;

    private AppUser makeUser(String uid, Set<String> roles) {
        AppUser u = new AppUser();
        u.setId(42L);
        u.setFirebaseUid(uid);
        u.setRoles(new HashSet<>(roles));
        return u;
    }

    @BeforeEach
    void setupUserContext() {
        when(userContextProvider.current()).thenReturn(new UserContext(1L, true, true));
    }

    @Test
    void getRoles_returns200_withRoles() throws Exception {
        String uid = "uid-1";
        AppUser user = makeUser(uid, Set.of("USER", "ADMIN"));
        when(appUserRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/admin/users/{uid}/roles", uid)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firebaseUid").value(uid))
            .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void addRoles_addsAndSyncs_returns200() throws Exception {
        String uid = "uid-2";
        AppUser user = makeUser(uid, Set.of("USER"));
        when(appUserRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/admin/users/{uid}/roles", uid)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"editor\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firebaseUid").value(uid))
            .andExpect(jsonPath("$.roles").isArray());

        verify(appUserRepository, atLeastOnce()).save(any(AppUser.class));
        verify(claimsSyncService).syncUserRolesByUid(uid, true);
    }

    @Test
    void addRoles_emptyBodyRoles_returns400InvalidArgument() throws Exception {
        String uid = "uid-3";
        AppUser user = makeUser(uid, Set.of("USER"));
        when(appUserRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/admin/users/{uid}/roles", uid)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void addRoles_unknownRole_returns400InvalidArgument() throws Exception {
        String uid = "uid-4";
        AppUser user = makeUser(uid, Set.of("USER"));
        when(appUserRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/admin/users/{uid}/roles", uid)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"superuser\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void removeRole_present_returns200_andSyncs() throws Exception {
        String uid = "uid-5";
        AppUser user = makeUser(uid, Set.of("USER", "EDITOR"));
        when(appUserRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/api/admin/users/{uid}/roles/{role}", uid, "editor")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.removed").value("EDITOR"))
            .andExpect(jsonPath("$.uid").value(uid));

        verify(appUserRepository, atLeastOnce()).save(any(AppUser.class));
        verify(claimsSyncService).syncUserRolesByUid(uid, true);
    }

    @Test
    void removeRole_absent_returns304() throws Exception {
        String uid = "uid-6";
        AppUser user = makeUser(uid, Set.of("USER"));
        when(appUserRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/api/admin/users/{uid}/roles/{role}", uid, "admin")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid))))
            .andExpect(status().isNotModified())
            .andExpect(jsonPath("$.message").value("Role not present"))
            .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(claimsSyncService, never()).syncUserRolesByUid(anyString(), anyBoolean());
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void syncRolesClaims_forceTrue_returns202_andCallsService() throws Exception {
        String uid = "uid-7";
        AppUser user = makeUser(uid, Set.of("USER"));
        when(appUserRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/admin/users/{uid}/roles/sync", uid)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid)))
                .param("force", "true"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.uid").value(uid))
            .andExpect(jsonPath("$.force").value(true))
            .andExpect(jsonPath("$.message").value("Role claims sync triggered"));

        verify(claimsSyncService).syncUserRolesByUid(uid, true);
    }

    @Test
    void operations_onUnknownUser_return404() throws Exception {
        String jwtUid = "jwt-known";
        String pathUid = "path-missing";
        // Filter uses jwt UID; controller uses path UID
        when(appUserRepository.findByFirebaseUid(jwtUid)).thenReturn(Optional.of(makeUser(jwtUid, Set.of("USER", "ADMIN"))));
        when(appUserRepository.findByFirebaseUid(pathUid)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/users/{uid}/roles", pathUid)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", jwtUid))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
