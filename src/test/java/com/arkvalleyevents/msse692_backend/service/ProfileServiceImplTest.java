package com.arkvalleyevents.msse692_backend.service;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.model.ProfileType;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.impl.ProfileServiceImpl;
import com.arkvalleyevents.msse692_backend.service.mapping.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private UserContextProvider userContextProvider;
    @Mock private ProfileMapper profileMapper;

    @InjectMocks private ProfileServiceImpl service;

    private AppUser user;

    @BeforeEach
    void setup() {
        user = new AppUser();
        user.setId(42L);
        user.setFirebaseUid("uid-42");
    }

    @Test
    @DisplayName("createCurrentProfile: VENUE requires non-empty location")
    void create_venueRequiresLocation() throws Exception {
        when(userContextProvider.requireUserId()).thenReturn(42L);
        when(profileRepository.existsByUserId(42L)).thenReturn(false);
        when(appUserRepository.findById(42L)).thenReturn(Optional.of(user));

        ProfileRequest req = buildRequest("My Venue", ProfileType.VENUE, null, "desc", List.of("x"), List.of());
        // mapper.toEntity should reflect VENUE type for rule check
        Profile mapped = new Profile();
        mapped.setProfileType(ProfileType.VENUE);
        when(profileMapper.toEntity(req)).thenReturn(mapped);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCurrentProfile(req));
        assertEquals("LOCATION_REQUIRED_FOR_VENUE", ex.getMessage());
        verify(profileRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createCurrentProfile: sets user, trims displayName, sets completed flag")
    void create_setsUserTrimsAndCompleted() throws Exception {
        when(userContextProvider.requireUserId()).thenReturn(42L);
        when(profileRepository.existsByUserId(42L)).thenReturn(false);
        when(appUserRepository.findById(42L)).thenReturn(Optional.of(user));

        ProfileRequest req = buildRequest("  Name  ", ProfileType.OTHER, "loc", "d", null, null);
        Profile mapped = new Profile();
        mapped.setDisplayName("  Name  ");
        mapped.setProfileType(ProfileType.OTHER);
        mapped.setLocation("loc");
        mapped.setDescription("d");
        when(profileMapper.toEntity(req)).thenReturn(mapped);

        ArgumentCaptor<Profile> saved = ArgumentCaptor.forClass(Profile.class);
        when(profileRepository.saveAndFlush(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Profile result = service.createCurrentProfile(req);

        verify(profileRepository).saveAndFlush(saved.capture());
        Profile p = saved.getValue();
        assertSame(user, p.getUser());
        assertEquals("Name", p.getDisplayName());
        assertTrue(p.isCompleted());
        assertEquals(ProfileType.OTHER, p.getProfileType());
        assertEquals("loc", p.getLocation());
        assertSame(p, result);
    }

    @Test
    @DisplayName("updateCurrentProfile: full replace copies fields from mapper")
    void update_fullReplaceCopiesFromMapper() throws Exception {
        when(userContextProvider.requireUserId()).thenReturn(42L);

        Profile existing = new Profile();
        existing.setId(1L);
        existing.setUser(user);
        existing.setDisplayName("Old");
        when(profileRepository.findByUserId(42L)).thenReturn(Optional.of(existing));

        ProfileRequest req = buildRequest(" New ", ProfileType.OTHER, " newloc ", "newd", List.of("a"), List.of("w"));
        Profile newVals = new Profile();
        newVals.setDisplayName(" New ");
        newVals.setProfileType(ProfileType.OTHER);
        newVals.setLocation(" newloc ");
        newVals.setDescription("newd");
        newVals.setSocials("[\"a\"]");
        newVals.setWebsites("[\"w\"]");
        when(profileMapper.toEntity(req)).thenReturn(newVals);

        when(profileRepository.saveAndFlush(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Profile result = service.updateCurrentProfile(req);

        assertEquals("New", existing.getDisplayName());
        assertEquals(ProfileType.OTHER, existing.getProfileType());
        assertEquals(" newloc ", existing.getLocation()); // trim only applies to display name; venue rule not applied
        assertEquals("newd", existing.getDescription());
        assertEquals("[\"a\"]", existing.getSocials());
        assertEquals("[\"w\"]", existing.getWebsites());
        assertSame(existing, result);
    }

    @Test
    @DisplayName("patchCurrentProfile: calls mapper.updateEntity and saves")
    void patch_callsMapperUpdateAndSaves() throws Exception {
        when(userContextProvider.requireUserId()).thenReturn(42L);
        Profile existing = new Profile();
        existing.setUser(user);
        existing.setDisplayName("Old");
        when(profileRepository.findByUserId(42L)).thenReturn(Optional.of(existing));

        ProfileRequest req = buildRequest("New", ProfileType.OTHER, null, null, null, null);
        when(profileRepository.saveAndFlush(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Profile result = service.patchCurrentProfile(req);

        verify(profileMapper).updateEntity(existing, req);
        assertSame(existing, result);
        verify(profileRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("deleteCurrentProfile: not found throws PROFILE_NOT_FOUND")
    void delete_notFound_throws() {
        when(userContextProvider.requireUserId()).thenReturn(42L);
        when(profileRepository.findByUserId(42L)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.deleteCurrentProfile());
        assertEquals("PROFILE_NOT_FOUND", ex.getMessage());
        verify(profileRepository, never()).delete(any());
    }

    // ===== Admin (by userId) tests =====

    @Test
    @DisplayName("createProfileForUser: already exists -> PROFILE_ALREADY_EXISTS")
    void adminCreate_alreadyExists_throws() throws Exception {
        long uid = 99L;
        when(profileRepository.existsByUserId(uid)).thenReturn(true);

        ProfileRequest req = buildRequest("Name", ProfileType.OTHER, null, null, null, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.createProfileForUser(uid, req));
        assertEquals("PROFILE_ALREADY_EXISTS", ex.getMessage());
        verify(profileRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createProfileForUser: VENUE sets location from request, trims name, sets completed")
    void adminCreate_venue_setsLocationAndCompleted() throws Exception {
        long uid = 77L;
        when(profileRepository.existsByUserId(uid)).thenReturn(false);
        AppUser target = new AppUser();
        target.setId(uid);
        when(appUserRepository.findById(uid)).thenReturn(Optional.of(target));

        ProfileRequest req = buildRequest("  Venue  ", ProfileType.VENUE, " City ", "bio", List.of("x"), List.of("w"));
        Profile mapped = new Profile();
        mapped.setProfileType(ProfileType.VENUE);
        mapped.setDisplayName("  Venue  ");
        // mapper location will be overridden by rule
        when(profileMapper.toEntity(req)).thenReturn(mapped);

        when(profileRepository.saveAndFlush(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Profile result = service.createProfileForUser(uid, req);

        assertSame(target, result.getUser());
        assertEquals("Venue", result.getDisplayName());
        assertTrue(result.isCompleted());
        assertEquals("City", result.getLocation());
    }

    @Test
    @DisplayName("updateProfileForUser: not found -> PROFILE_NOT_FOUND")
    void adminUpdate_notFound_throws() throws Exception {
        long uid = 77L;
        when(profileRepository.findByUserId(uid)).thenReturn(Optional.empty());

        ProfileRequest req = buildRequest("Name", ProfileType.OTHER, null, null, null, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.updateProfileForUser(uid, req));
        assertEquals("PROFILE_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("updateProfileForUser: copies from mapper and enforces venue rule")
    void adminUpdate_copiesAndEnforcesVenueRule() throws Exception {
        long uid = 77L;
        Profile existing = new Profile();
        existing.setUser(user);
        existing.setDisplayName("Old");
        when(profileRepository.findByUserId(uid)).thenReturn(Optional.of(existing));

        ProfileRequest req = buildRequest(" New ", ProfileType.VENUE, " City ", "desc", List.of("s"), List.of("w"));
        Profile newVals = new Profile();
        newVals.setDisplayName(" New ");
        newVals.setProfileType(ProfileType.VENUE);
        newVals.setLocation(" old ");
        newVals.setDescription("desc");
        newVals.setSocials("[\"s\"]");
        newVals.setWebsites("[\"w\"]");
        when(profileMapper.toEntity(req)).thenReturn(newVals);

        when(profileRepository.saveAndFlush(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Profile out = service.updateProfileForUser(uid, req);

        assertEquals("New", existing.getDisplayName());
        assertEquals(ProfileType.VENUE, existing.getProfileType());
        // venue rule sets from request and trims
        assertEquals("City", existing.getLocation());
        assertEquals("desc", existing.getDescription());
        assertEquals("[\"s\"]", existing.getSocials());
        assertEquals("[\"w\"]", existing.getWebsites());
        assertSame(existing, out);
    }

    @Test
    @DisplayName("patchProfileForUser: not found -> PROFILE_NOT_FOUND")
    void adminPatch_notFound_throws() throws Exception {
        long uid = 77L;
        when(profileRepository.findByUserId(uid)).thenReturn(Optional.empty());
        ProfileRequest req = buildRequest(null, ProfileType.OTHER, null, null, null, null);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.patchProfileForUser(uid, req));
        assertEquals("PROFILE_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("patchProfileForUser: calls mapper.updateEntity and saves; venue rule applies")
    void adminPatch_callsMapperAndSaves() throws Exception {
        long uid = 77L;
        Profile existing = new Profile();
        existing.setProfileType(ProfileType.VENUE);
        when(profileRepository.findByUserId(uid)).thenReturn(Optional.of(existing));

        ProfileRequest req = buildRequest(null, ProfileType.VENUE, " Metropolis ", null, null, null);
        when(profileRepository.saveAndFlush(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Profile out = service.patchProfileForUser(uid, req);

        verify(profileMapper).updateEntity(existing, req);
        assertEquals("Metropolis", existing.getLocation());
        assertSame(existing, out);
    }

    @Test
    @DisplayName("deleteProfileForUser: success deletes entity")
    void adminDelete_success() {
        long uid = 77L;
        Profile existing = new Profile();
        when(profileRepository.findByUserId(uid)).thenReturn(Optional.of(existing));

        service.deleteProfileForUser(uid);

        verify(profileRepository).delete(existing);
    }

    // -------- helpers --------
    private ProfileRequest buildRequest(String displayName, ProfileType type, String location, String description,
                                        List<String> socials, List<String> websites) throws Exception {
        ProfileRequest req = new ProfileRequest();
        set(req, "displayName", displayName);
        set(req, "profileType", type);
        set(req, "location", location);
        set(req, "description", description);
        set(req, "socials", socials);
        set(req, "websites", websites);
        return req;
    }

    private void set(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
