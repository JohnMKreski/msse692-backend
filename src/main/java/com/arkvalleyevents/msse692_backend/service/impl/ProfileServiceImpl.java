package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.model.ProfileType;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import com.arkvalleyevents.msse692_backend.service.ProfileService;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.mapping.ProfileMapper;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final AppUserRepository appUserRepository;
    private final UserContextProvider userContextProvider;
    private final ProfileMapper profileMapper;

    public ProfileServiceImpl(ProfileRepository profileRepository, AppUserRepository appUserRepository, UserContextProvider userContextProvider, ProfileMapper profileMapper) {
        this.profileRepository = profileRepository;
        this.appUserRepository = appUserRepository;
        this.userContextProvider = userContextProvider;
        this.profileMapper = profileMapper;
    }

    @Override
    public Optional<Profile> getCurrentProfile(String firebaseUid) {
        return profileRepository.findByUserFirebaseUid(firebaseUid);
    }

    @Override
    @Transactional
    public Profile upsertProfile(String firebaseUid, ProfileRequest request) {
        AppUser user = appUserRepository.findByFirebaseUid(firebaseUid)
            .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));

        Optional<Profile> existing = profileRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            Profile p = existing.get();
            // Apply partial update via mapper (nulls ignored)
            profileMapper.updateEntity(p, request);
            trimDisplayName(p);
            enforceVenueLocationRule(p, request);
            updateCompletedFlag(p);
            return profileRepository.saveAndFlush(p);
        } else {
            Profile p = profileMapper.toEntity(request);
            p.setUser(user);
            trimDisplayName(p);
            enforceVenueLocationRule(p, request);
            updateCompletedFlag(p);
            return profileRepository.saveAndFlush(p);
        }
    }

    // Preferred context-based methods
    @Override
    public Optional<Profile> getCurrentProfile() {
        Long userId = userContextProvider.requireUserId();
        return profileRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Profile upsertCurrentProfile(ProfileRequest request) {
        Long userId = userContextProvider.requireUserId();
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));

        Optional<Profile> existing = profileRepository.findByUserId(userId);
        if (existing.isPresent()) {
            Profile p = existing.get();
            profileMapper.updateEntity(p, request);
            trimDisplayName(p);
            enforceVenueLocationRule(p, request);
            updateCompletedFlag(p);
            return profileRepository.saveAndFlush(p);
        } else {
            Profile p = profileMapper.toEntity(request);
            p.setUser(user);
            trimDisplayName(p);
            enforceVenueLocationRule(p, request);
            updateCompletedFlag(p);
            return profileRepository.saveAndFlush(p);
        }
    }

    private void trimDisplayName(Profile p) {
        if (StringUtils.hasText(p.getDisplayName())) {
            p.setDisplayName(p.getDisplayName().trim());
        }
    }

    private void enforceVenueLocationRule(Profile p, ProfileRequest request) {
        if (p.getProfileType() == ProfileType.VENUE) {
            String loc = request.getLocation();
            if (!StringUtils.hasText(loc)) {
                throw new IllegalArgumentException("LOCATION_REQUIRED_FOR_VENUE");
            }
            p.setLocation(loc.trim());
        }
    }

    private void updateCompletedFlag(Profile p) {
        p.setCompleted(StringUtils.hasText(p.getDisplayName()));
    }

    // ===== Admin (by userId) CRUD =====
    @Override
    public Optional<Profile> getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Profile createProfileForUser(Long userId, ProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("PROFILE_ALREADY_EXISTS");
        }
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));
        Profile p = profileMapper.toEntity(request);
        p.setUser(user);
        trimDisplayName(p);
        enforceVenueLocationRule(p, request);
        updateCompletedFlag(p);
        return profileRepository.saveAndFlush(p);
    }

    @Override
    @Transactional
    public Profile updateProfileForUser(Long userId, ProfileRequest request) {
        Profile existing = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("PROFILE_NOT_FOUND"));
        Profile newVals = profileMapper.toEntity(request);
        existing.setDisplayName(newVals.getDisplayName());
        existing.setProfileType(newVals.getProfileType());
        existing.setLocation(newVals.getLocation());
        existing.setDescription(newVals.getDescription());
        existing.setSocials(newVals.getSocials());
        existing.setWebsites(newVals.getWebsites());
        trimDisplayName(existing);
        enforceVenueLocationRule(existing, request);
        updateCompletedFlag(existing);
        return profileRepository.saveAndFlush(existing);
    }

    @Override
    @Transactional
    public Profile patchProfileForUser(Long userId, ProfileRequest request) {
        Profile existing = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("PROFILE_NOT_FOUND"));
        profileMapper.updateEntity(existing, request);
        trimDisplayName(existing);
        enforceVenueLocationRule(existing, request);
        updateCompletedFlag(existing);
        return profileRepository.saveAndFlush(existing);
    }

    @Override
    @Transactional
    public void deleteProfileForUser(Long userId) {
        Profile existing = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("PROFILE_NOT_FOUND"));
        profileRepository.delete(existing);
    }

    // ===== Explicit CRUD methods =====
    @Override
    @Transactional
    public Profile createCurrentProfile(ProfileRequest request) {
        Long userId = userContextProvider.requireUserId();
        if (profileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("PROFILE_ALREADY_EXISTS");
        }
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));
        Profile p = profileMapper.toEntity(request);
        p.setUser(user);
        trimDisplayName(p);
        enforceVenueLocationRule(p, request);
        updateCompletedFlag(p);
        return profileRepository.saveAndFlush(p);
    }

    @Override
    @Transactional
    public Profile updateCurrentProfile(ProfileRequest request) {
        Long userId = userContextProvider.requireUserId();
        Profile existing = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("PROFILE_NOT_FOUND"));
        // Build new values from DTO then copy across (full update semantics)
        Profile newVals = profileMapper.toEntity(request);
        // Copy mutable fields (exclude id, user, verified flag, timestamps handled by entity events)
        existing.setDisplayName(newVals.getDisplayName());
        existing.setProfileType(newVals.getProfileType());
        existing.setLocation(newVals.getLocation());
        existing.setDescription(newVals.getDescription());
        existing.setSocials(newVals.getSocials());
        existing.setWebsites(newVals.getWebsites());
        trimDisplayName(existing);
        enforceVenueLocationRule(existing, request);
        updateCompletedFlag(existing);
        return profileRepository.saveAndFlush(existing);
    }

    @Override
    @Transactional
    public Profile patchCurrentProfile(ProfileRequest request) {
        Long userId = userContextProvider.requireUserId();
        Profile existing = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("PROFILE_NOT_FOUND"));
        profileMapper.updateEntity(existing, request);
        trimDisplayName(existing);
        enforceVenueLocationRule(existing, request);
        updateCompletedFlag(existing);
        return profileRepository.saveAndFlush(existing);
    }

    @Override
    @Transactional
    public void deleteCurrentProfile() {
        Long userId = userContextProvider.requireUserId();
        Profile existing = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("PROFILE_NOT_FOUND"));
        profileRepository.delete(existing);
    }
}
