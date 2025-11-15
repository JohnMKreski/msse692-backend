package com.arkvalleyevents.msse692_backend.controller;

 
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.service.ProfileService;
import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc // enable security filters so jwt() request post processor populates SecurityContext
@SuppressWarnings("null")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Autowired
    private ProfileControllerTest.FakeProfileService profileService;

    // Mock repository beans so the AppUserUpsertFilter (component) can be constructed without failing dependency lookup
    @MockitoBean
    private AppUserRepository appUserRepository;
    @MockitoBean
    private ProfileRepository profileRepository;

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
                .content("{\"displayName\":\"New Name\"}")
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
                .content("{\"displayName\":\"Updated Name\"}")
                .with(jwt().jwt(j -> j.claim("sub", "uid-existing"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Updated Name"));
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

        void setNextGetCurrent(Optional<Profile> p) { this.nextGetCurrent = p; }
        void setNextUpsert(Profile p) { this.nextUpsert = p; }

        @Override
        public Optional<Profile> getCurrentProfile(String firebaseUid) { throw new UnsupportedOperationException(); }

        @Override
        public Profile upsertProfile(String firebaseUid, ProfileRequest request) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<Profile> getCurrentProfile() { return nextGetCurrent; }

        @Override
        public Profile upsertCurrentProfile(ProfileRequest request) { return nextUpsert; }
    }
}
