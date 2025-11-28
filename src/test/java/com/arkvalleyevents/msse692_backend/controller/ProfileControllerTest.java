package com.arkvalleyevents.msse692_backend.controller;

 
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.service.ProfileService;
import com.arkvalleyevents.msse692_backend.service.mapping.ProfileMapper;
import com.arkvalleyevents.msse692_backend.dto.response.ProfileResponse;
import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.BeforeEach;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc // enable security filters so jwt() request post processor populates SecurityContext
@SuppressWarnings("null")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Autowired
    private ProfileControllerTest.FakeProfileService profileService;

    @MockitoBean
    private ProfileMapper profileMapper;

    // Mock repository beans so the AppUserUpsertFilter (component) can be constructed without failing dependency lookup
    @MockitoBean
    private AppUserRepository appUserRepository;
    @MockitoBean
    private ProfileRepository profileRepository;

    @BeforeEach
    void defaultMapperSetup() {
        org.mockito.Mockito.when(profileMapper.toResponse(org.mockito.ArgumentMatchers.any(Profile.class)))
            .thenAnswer(inv -> {
                Profile p = inv.getArgument(0);
                ProfileResponse r = new ProfileResponse();
                if (p.getUser() != null) r.setUserId(p.getUser().getId());
                r.setDisplayName(p.getDisplayName());
                r.setCompleted(p.isCompleted());
                r.setVerified(p.isVerified());
                r.setCreatedAt(p.getCreatedAt());
                r.setUpdatedAt(p.getUpdatedAt());
                r.setProfileType(p.getProfileType());
                r.setLocation(p.getLocation());
                r.setDescription(p.getDescription());
                return r;
            });
    }

    @Test
    void getProfile_notFound_returns404() throws Exception {
        profileService.setNextGetCurrent(Optional.empty());

    mockMvc.perform(get("/api/v1/profile/me")
                .with(jwt().jwt(j -> j.claim("sub", "firebase-123"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void getProfile_found_returns200() throws Exception {
        Profile p = new Profile();
        AppUser u = new AppUser();
        u.setId(10L);
        p.setUser(u);
        p.setDisplayName("Display");
        p.setCompleted(true);
        p.setVerified(false);
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
    profileService.setNextGetCurrent(Optional.of(p));

    mockMvc.perform(get("/api/v1/profile/me")
        .with(jwt().jwt(j -> j.claim("sub", "uid-456"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Display"))
            .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void postProfile_create_returns201() throws Exception {
        profileService.setNextGetCurrent(Optional.empty());
        Profile p = new Profile();
        AppUser u = new AppUser();
        u.setId(77L);
        p.setUser(u);
        p.setDisplayName("New Name");
        p.setCompleted(true);
        p.setVerified(false);
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
    profileService.setNextUpsert(p);

    mockMvc.perform(post("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"New Name\",\"profileType\":\"OTHER\"}")
        .with(jwt().jwt(j -> j.claim("sub", "uid-new"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.displayName").value("New Name"));
    }

    @Test
    void postProfile_update_returns200() throws Exception {
        Profile existing = new Profile();
        AppUser u = new AppUser();
        u.setId(5L);
        existing.setUser(u);
        existing.setDisplayName("Old Name");
        existing.setCompleted(true);
        existing.setVerified(false);
        existing.setCreatedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());
        profileService.setNextGetCurrent(Optional.of(existing));

        Profile updated = new Profile();
        updated.setUser(u);
        updated.setDisplayName("Updated Name");
        updated.setCompleted(true);
        updated.setVerified(false);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(OffsetDateTime.now());
        profileService.setNextUpsert(updated);

        mockMvc.perform(post("/api/v1/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"Updated Name\",\"profileType\":\"OTHER\"}")
            .with(jwt().jwt(j -> j.claim("sub", "uid-existing"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Updated Name"));
    }

    @Test
    void createMyProfile_returns201() throws Exception {
        Profile created = new Profile();
        AppUser u = new AppUser();
        u.setId(11L);
        created.setUser(u);
        created.setDisplayName("Mine");
        profileService.setNextCreate(created);

        mockMvc.perform(post("/api/v1/profile/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Mine\",\"profileType\":\"OTHER\"}")
                .with(jwt()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.displayName").value("Mine"));
    }

    @Test
    void putMyProfile_returns200() throws Exception {
        Profile updated = new Profile();
        updated.setDisplayName("Full");
        profileService.setNextUpdate(updated);

        mockMvc.perform(put("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Full\",\"profileType\":\"OTHER\"}")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Full"));
    }

    @Test
    void patchMyProfile_returns200_withoutValidation() throws Exception {
        Profile patched = new Profile();
        patched.setDisplayName("Keep");
        profileService.setNextPatch(patched);

        mockMvc.perform(patch("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"bio\"}")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Keep"));
    }

    @Test
    void deleteMyProfile_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/profile").with(jwt()))
            .andExpect(status().isNoContent());
    }

    @Test
    void getMyProfile_unauthorized_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me"))
            .andExpect(status().isUnauthorized());
    }

    // ---- Admin endpoints ----
    @Test
    void admin_getProfileByUserId_unauthenticated_returns401() throws Exception {
        // unauthorized when no JWT
        mockMvc.perform(get("/api/v1/profile/55"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_getProfileByUserId_returns200() throws Exception {
        Profile p = new Profile();
        AppUser au = new AppUser();
        au.setId(55L);
        p.setUser(au);
        p.setDisplayName("AdminView");
        profileService.setNextAdminGet(Optional.of(p));

        mockMvc.perform(get("/api/v1/profile/55")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("AdminView"));
    }

    @Test
    void admin_createUpdateDelete_succeeds() throws Exception {
        Profile created = new Profile();
        created.setDisplayName("C");
        profileService.setNextAdminCreate(created);

        Profile updated = new Profile();
        updated.setDisplayName("U");
        profileService.setNextAdminUpdate(updated);

        Profile patched = new Profile();
        patched.setDisplayName("P");
        profileService.setNextAdminPatch(patched);

        // create
        mockMvc.perform(post("/api/v1/profile/77/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"C\",\"profileType\":\"OTHER\"}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.displayName").value("C"));

        // update
        mockMvc.perform(put("/api/v1/profile/77")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"U\",\"profileType\":\"OTHER\"}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("U"));

        // patch
        mockMvc.perform(patch("/api/v1/profile/77")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"d\"}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("P"));

        // delete
        mockMvc.perform(delete("/api/v1/profile/77")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isNoContent());
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class StubConfig {
        @org.springframework.context.annotation.Bean
        ProfileService profileServiceBean() {
            return new FakeProfileService();
        }
    }

    static class FakeProfileService implements ProfileService {
        private Optional<Profile> nextGetCurrent = Optional.empty();
        private Profile nextUpsert;
        private Profile nextCreate;
        private Profile nextUpdate;
        private Profile nextPatch;
        private Optional<Profile> nextAdminGet = Optional.empty();
        private Profile nextAdminCreate;
        private Profile nextAdminUpdate;
        private Profile nextAdminPatch;

        void setNextGetCurrent(Optional<Profile> p) { this.nextGetCurrent = p; }
        void setNextUpsert(Profile p) { this.nextUpsert = p; }
        void setNextCreate(Profile p) { this.nextCreate = p; }
        void setNextUpdate(Profile p) { this.nextUpdate = p; }
        void setNextPatch(Profile p) { this.nextPatch = p; }
        void setNextAdminGet(Optional<Profile> p) { this.nextAdminGet = p; }
        void setNextAdminCreate(Profile p) { this.nextAdminCreate = p; }
        void setNextAdminUpdate(Profile p) { this.nextAdminUpdate = p; }
        void setNextAdminPatch(Profile p) { this.nextAdminPatch = p; }

        @Override
        public Optional<Profile> getCurrentProfile(String firebaseUid) { throw new UnsupportedOperationException(); }

        @Override
        public Profile upsertProfile(String firebaseUid, ProfileRequest request) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<Profile> getCurrentProfile() { return nextGetCurrent; }

        @Override
        public Profile upsertCurrentProfile(ProfileRequest request) { return nextUpsert; }

        // ===== Added interface methods (not exercised by these tests) =====
        @Override
        public Profile createCurrentProfile(ProfileRequest request) { return nextCreate != null ? nextCreate : nextUpsert; }

        @Override
        public Profile updateCurrentProfile(ProfileRequest request) { return nextUpdate != null ? nextUpdate : nextUpsert; }

        @Override
        public Profile patchCurrentProfile(ProfileRequest request) { return nextPatch != null ? nextPatch : nextUpsert; }

        @Override
        public void deleteCurrentProfile() { /* no-op for test */ }

        @Override
        public Optional<Profile> getProfileByUserId(Long userId) { return nextAdminGet; }

        @Override
        public Profile createProfileForUser(Long userId, ProfileRequest request) { return nextAdminCreate; }

        @Override
        public Profile updateProfileForUser(Long userId, ProfileRequest request) { return nextAdminUpdate; }

        @Override
        public Profile patchProfileForUser(Long userId, ProfileRequest request) { return nextAdminPatch; }

        @Override
        public void deleteProfileForUser(Long userId) { /* no-op */ }
    }
}
