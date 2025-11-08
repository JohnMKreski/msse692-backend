package com.arkvalleyevents.msse692_backend.service;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.model.Profile;
import java.util.Optional;

public interface ProfileService {
    Optional<Profile> getCurrentProfile(String firebaseUid);
    Profile upsertProfile(String firebaseUid, ProfileRequest request);
}
