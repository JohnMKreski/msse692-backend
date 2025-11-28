package com.arkvalleyevents.msse692_backend.service;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.model.Profile;
import java.util.Optional;

public interface ProfileService {
    Optional<Profile> getCurrentProfile(String firebaseUid);
    Profile upsertProfile(String firebaseUid, ProfileRequest request);

    // Context-based convenience methods (preferred)
    Optional<Profile> getCurrentProfile();
    Profile upsertCurrentProfile(ProfileRequest request);

    // ---- Explicit CRUD for current user's profile ----
    Profile createCurrentProfile(ProfileRequest request);
    Profile updateCurrentProfile(ProfileRequest request); // full replace semantics
    Profile patchCurrentProfile(ProfileRequest request); // partial (ignore nulls)
    void deleteCurrentProfile();

    // ---- Admin (by userId) CRUD ----
    Optional<Profile> getProfileByUserId(Long userId);
    Profile createProfileForUser(Long userId, ProfileRequest request);
    Profile updateProfileForUser(Long userId, ProfileRequest request);
    Profile patchProfileForUser(Long userId, ProfileRequest request);
    void deleteProfileForUser(Long userId);
}
