package com.arkvalleyevents.msse692_backend.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

import com.arkvalleyevents.msse692_backend.repository.AppUserRepository; // retained for filters
import com.arkvalleyevents.msse692_backend.service.RoleRequestService;
import com.arkvalleyevents.msse692_backend.service.UserRoleService;
import com.arkvalleyevents.msse692_backend.dto.response.RoleRequestDto;
import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.EntityNotFoundException;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.FirebaseClaimsSyncService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc
@Import(com.arkvalleyevents.msse692_backend.config.SecurityConfig.class)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://test-issuer"
})
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

    @MockitoBean
    private RoleRequestService roleRequestService;

    @MockitoBean
    private UserRoleService userRoleService;

    @MockitoBean
    private JwtDecoder jwtDecoder; // override real decoder to avoid network calls in dev profile

    @BeforeEach
    void setupUserContext() {
        when(userContextProvider.current()).thenReturn(new UserContext(1L, true, true));
    }

    @Test
    void getRoles_returns200_withRoles() throws Exception {
        String uid = "uid-1";
        when(userRoleService.getRoles(uid)).thenReturn(new UserRoleService.RolesView(uid, Set.of("USER", "ADMIN")));

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
        when(userRoleService.addRoles(eq(uid), anySet())).thenReturn(new UserRoleService.RolesView(uid, Set.of("USER", "EDITOR")));

        mockMvc.perform(post("/api/admin/users/{uid}/roles", uid)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"editor\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firebaseUid").value(uid))
            .andExpect(jsonPath("$.roles").isArray());

        verify(userRoleService).addRoles(eq(uid), anySet());
    }

    @Test
    void addRoles_emptyBodyRoles_returns400InvalidArgument() throws Exception {
        String uid = "uid-3";
        // service will throw because roles set empty
        when(userRoleService.addRoles(eq(uid), eq(Set.of()))).thenThrow(new IllegalArgumentException("No roles supplied"));

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
        when(userRoleService.addRoles(eq(uid), anySet())).thenThrow(new IllegalArgumentException("Unknown role"));

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
        when(userRoleService.removeRole(uid, "editor")).thenReturn(new UserRoleService.RemoveRoleResult(true, "EDITOR", uid));

        mockMvc.perform(delete("/api/admin/users/{uid}/roles/{role}", uid, "editor")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.removed").value("EDITOR"))
            .andExpect(jsonPath("$.uid").value(uid));

        verify(userRoleService).removeRole(uid, "editor");
    }

    @Test
    void removeRole_absent_returns304() throws Exception {
        String uid = "uid-6";
        when(userRoleService.removeRole(uid, "admin")).thenReturn(new UserRoleService.RemoveRoleResult(false, "ADMIN", uid));

        mockMvc.perform(delete("/api/admin/users/{uid}/roles/{role}", uid, "admin")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid))))
            .andExpect(status().isNotModified())
            .andExpect(jsonPath("$.message").value("Role not present"))
            .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(userRoleService).removeRole(uid, "admin");
    }

    @Test
    void syncRolesClaims_forceTrue_returns202_andCallsService() throws Exception {
        String uid = "uid-7";
        when(userRoleService.syncClaims(uid, true)).thenReturn(new UserRoleService.SyncResult(uid, true));

        mockMvc.perform(post("/api/admin/users/{uid}/roles/sync", uid)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", uid)))
                .param("force", "true"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.uid").value(uid))
            .andExpect(jsonPath("$.force").value(true))
            .andExpect(jsonPath("$.message").value("Role claims sync triggered"));

        verify(userRoleService).syncClaims(uid, true);
    }

    @Test
    void operations_onUnknownUser_return404() throws Exception {
        String jwtUid = "jwt-known";
        String pathUid = "path-missing";
        when(userRoleService.getRoles(pathUid)).thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(get("/api/admin/users/{uid}/roles", pathUid)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(j -> j.claim("sub", jwtUid))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

        // -------------------------------------------------------------
        // Role Request Admin Endpoints (list, detail, approve, reject)
        // -------------------------------------------------------------

        private RoleRequestDto makeDto(String id, String requesterUid, RoleRequestStatus status) {
        RoleRequestDto dto = new RoleRequestDto();
        dto.setId(id);
        dto.setRequesterUid(requesterUid);
        dto.setRequestedRoles(java.util.List.of("EDITOR"));
        dto.setStatus(status);
        return dto;
        }

        @Test
        void adminList_returnsPendingRequest() throws Exception {
        String id = java.util.UUID.randomUUID().toString();
        RoleRequestDto dto = makeDto(id, "req-1", RoleRequestStatus.PENDING);
        when(roleRequestService.adminList(any(), any(), any(Pageable.class)))
            .thenAnswer(inv -> new PageImpl<>(java.util.List.of(dto), (Pageable) inv.getArgument(2), 1));

        mockMvc.perform(get("/api/admin/users/roles/requests")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .jwt(j -> j.claim("sub", "admin-user")))
            .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(id))
            .andExpect(jsonPath("$.content[0].status").value("Pending"));
        }

        @Test
        void adminDetail_returnsRequest() throws Exception {
        String id = java.util.UUID.randomUUID().toString();
        RoleRequestDto dto = makeDto(id, "req-detail", RoleRequestStatus.PENDING);
        when(roleRequestService.get(java.util.UUID.fromString(id))).thenReturn(dto);

        mockMvc.perform(get("/api/admin/users/roles/requests/{id}", java.util.UUID.fromString(id))
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .jwt(j -> j.claim("sub", "admin-user"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.status").value("Pending"));
        }

        @Test
        void adminApprove_transitionsToApproved() throws Exception {
        String id = java.util.UUID.randomUUID().toString();
        RoleRequestDto approved = makeDto(id, "req-app", RoleRequestStatus.APPROVED);
        approved.setApproverUid("admin-user");
        when(roleRequestService.approve(eq(java.util.UUID.fromString(id)), eq("admin-user"), any()))
            .thenReturn(approved);

        mockMvc.perform(post("/api/admin/users/roles/requests/{id}/approve", java.util.UUID.fromString(id))
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .jwt(j -> j.claim("sub", "admin-user")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"approverNote\":\"Looks good\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Approved"))
            .andExpect(jsonPath("$.approverUid").value("admin-user"));
        }

        @Test
        void adminReject_transitionsToRejected() throws Exception {
        String id = java.util.UUID.randomUUID().toString();
        RoleRequestDto rejected = makeDto(id, "req-rej", RoleRequestStatus.REJECTED);
        rejected.setApproverUid("admin-user");
        when(roleRequestService.reject(eq(java.util.UUID.fromString(id)), eq("admin-user"), any()))
            .thenReturn(rejected);

        mockMvc.perform(post("/api/admin/users/roles/requests/{id}/reject", java.util.UUID.fromString(id))
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .jwt(j -> j.claim("sub", "admin-user")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"approverNote\":\"Insufficient detail\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Rejected"))
            .andExpect(jsonPath("$.approverUid").value("admin-user"));
        }

        @Test
        void adminApprove_nonPending_returns409() throws Exception {
        String id = java.util.UUID.randomUUID().toString();
        when(roleRequestService.approve(eq(java.util.UUID.fromString(id)), eq("admin-user"), any()))
            .thenThrow(new IllegalStateException("Only PENDING requests can be approved"));

        mockMvc.perform(post("/api/admin/users/roles/requests/{id}/approve", java.util.UUID.fromString(id))
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .jwt(j -> j.claim("sub", "admin-user")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"approverNote\":\"note\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ILLEGAL_STATE"))
            .andExpect(jsonPath("$.message").value(containsString("Only PENDING")));
        }

        @Test
        void adminReject_nonPending_returns409() throws Exception {
        String id = java.util.UUID.randomUUID().toString();
        when(roleRequestService.reject(eq(java.util.UUID.fromString(id)), eq("admin-user"), any()))
            .thenThrow(new IllegalStateException("Only PENDING requests can be rejected"));

        mockMvc.perform(post("/api/admin/users/roles/requests/{id}/reject", java.util.UUID.fromString(id))
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .jwt(j -> j.claim("sub", "admin-user")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"approverNote\":\"note\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ILLEGAL_STATE"))
            .andExpect(jsonPath("$.message").value(containsString("Only PENDING")));
        }

        @Test
        void adminDetail_notFound_returns404() throws Exception {
        String id = java.util.UUID.randomUUID().toString();
        when(roleRequestService.get(java.util.UUID.fromString(id)))
            .thenThrow(new EntityNotFoundException("Role request not found"));

        mockMvc.perform(get("/api/admin/users/roles/requests/{id}", java.util.UUID.fromString(id))
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .jwt(j -> j.claim("sub", "admin-user"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        void adminList_forbiddenWithoutAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users/roles/requests")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .jwt(j -> j.claim("sub", "some-user"))))
            .andExpect(status().isForbidden());
        }

        @Test
        void adminDetail_invalidUuid_returns400TypeMismatch() throws Exception {
        mockMvc.perform(get("/api/admin/users/roles/requests/{id}", "not-a-uuid")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .jwt(j -> j.claim("sub", "admin-user"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));
        }
}
