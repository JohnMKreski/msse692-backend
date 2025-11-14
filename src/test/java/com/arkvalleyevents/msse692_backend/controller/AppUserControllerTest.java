package com.arkvalleyevents.msse692_backend.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.AppUserService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppUserController.class)
@AutoConfigureMockMvc
@SuppressWarnings("null")
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private ProfileRepository profileRepository; // required by AppUserUpsertFilter

    @MockitoBean
    private UserContextProvider userContextProvider;

    @MockitoBean
    private AppUserService appUserService;

    @BeforeEach
    void setupDefaults() {
        when(userContextProvider.current()).thenReturn(new UserContext(10L, false, false));
    }

    @Test
    void me_found_returns200() throws Exception {
        // AppUserUpsertFilter will look up by firebase uid; stub to an existing user to avoid creation
        String uid = "jwt-uid";
        AppUser existing = new AppUser();
        existing.setId(10L);
        existing.setFirebaseUid(uid);
        existing.setRoles(Set.of("USER"));
        when(appUserRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(existing));

        // Controller delegates to service
        when(appUserService.getCurrentUser()).thenReturn(existing);

        mockMvc.perform(get("/api/v1/app-users/me")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", uid))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.firebaseUid").value(uid));
    }

    @Test
    void me_notFound_returns404() throws Exception {
        String uid = "jwt-uid-2";
        AppUser existing = new AppUser();
        existing.setId(77L);
        existing.setFirebaseUid(uid);
        existing.setRoles(Set.of("USER"));
        // Filter path
        when(appUserRepository.findByFirebaseUid(uid)).thenReturn(Optional.of(existing));
        // Service throws not found for current user
        when(appUserService.getCurrentUser()).thenThrow(new jakarta.persistence.EntityNotFoundException("No app user exists for this token."));

        mockMvc.perform(get("/api/v1/app-users/me")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", uid))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
