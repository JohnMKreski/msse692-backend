package com.arkvalleyevents.msse692_backend.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@SuppressWarnings("null")
class AppUserUpsertFilterTest {

    private AppUserRepository appUserRepository;
    private ProfileRepository profileRepository;
    private AppUserUpsertFilter filter;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        profileRepository = mock(ProfileRepository.class);
        filter = new AppUserUpsertFilter(appUserRepository, profileRepository);
        // Ensure clean SecurityContext
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void runFilterWithJwt(String uid, String email, String name, String picture) throws Exception {
        Jwt jwt = Jwt.withTokenValue("tkn")
            .header("alg", "none")
            .claim("sub", uid)
            .claim("email", email)
            .claim("name", name)
            .claim("picture", picture)
            .build();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void createsUserWhenAbsent() throws Exception {
        when(appUserRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());
        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        runFilterWithJwt("uid-1", "a@b.com", "Alice", "http://pic");

        verify(appUserRepository, times(1)).save(saved.capture());
        AppUser u = saved.getValue();
        assertEquals("uid-1", u.getFirebaseUid());
        assertEquals("a@b.com", u.getEmail());
        assertEquals("Alice", u.getDisplayName());
        assertEquals("http://pic", u.getPhotoUrl());
        assertTrue(u.getRoles() != null && u.getRoles().contains("USER"));
    }

    @Test
    void updatesFieldsWhenNoCompletedProfile() throws Exception {
        AppUser existing = new AppUser();
        existing.setId(10L);
        existing.setFirebaseUid("uid-2");
        existing.setDisplayName("Old");
        existing.setEmail(null);
        existing.setPhotoUrl(null);
        existing.setRoles(new HashSet<>());
        when(appUserRepository.findByFirebaseUid("uid-2")).thenReturn(Optional.of(existing));
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        runFilterWithJwt("uid-2", "b@c.com", "Bob", "http://pic2");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(captor.capture());
        AppUser updated = captor.getValue();
        assertEquals("b@c.com", updated.getEmail());
        assertEquals("Bob", updated.getDisplayName(), "displayName should update when no completed profile");
        assertEquals("http://pic2", updated.getPhotoUrl());
    }

    @Test
    void doesNotOverwriteDisplayNameWhenCompletedProfileExists() throws Exception {
        AppUser existing = new AppUser();
        existing.setId(11L);
        existing.setFirebaseUid("uid-3");
        existing.setDisplayName("KeepMe");
        existing.setEmail("old@x.com");
        existing.setPhotoUrl("oldpic");
        existing.setRoles(new HashSet<>());

        when(appUserRepository.findByFirebaseUid("uid-3")).thenReturn(Optional.of(existing));
        Profile prof = new Profile();
        prof.setCompleted(true);
        when(profileRepository.findByUserId(11L)).thenReturn(Optional.of(prof));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        runFilterWithJwt("uid-3", "new@x.com", "NewName", "newpic");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(captor.capture());
        AppUser updated = captor.getValue();
        assertEquals("new@x.com", updated.getEmail());
        assertEquals("KeepMe", updated.getDisplayName(), "displayName must NOT change when completed profile exists");
        assertEquals("newpic", updated.getPhotoUrl());
    }

    @Test
    void overwritesDisplayNameWhenProfileNotCompleted() throws Exception {
        AppUser existing = new AppUser();
        existing.setId(12L);
        existing.setFirebaseUid("uid-4");
        existing.setDisplayName("OldName");
        existing.setRoles(new HashSet<>());
        when(appUserRepository.findByFirebaseUid("uid-4")).thenReturn(Optional.of(existing));
        Profile prof = new Profile();
        prof.setCompleted(false);
        when(profileRepository.findByUserId(12L)).thenReturn(Optional.of(prof));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        runFilterWithJwt("uid-4", "user@d.com", "NewName", "pic");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(captor.capture());
        AppUser updated = captor.getValue();
        assertEquals("NewName", updated.getDisplayName(), "displayName should update when profile not completed");
    }
}
