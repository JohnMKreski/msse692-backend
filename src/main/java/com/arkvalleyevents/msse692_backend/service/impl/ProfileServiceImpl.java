package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import com.arkvalleyevents.msse692_backend.service.ProfileService;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final AppUserRepository appUserRepository;

    public ProfileServiceImpl(ProfileRepository profileRepository, AppUserRepository appUserRepository) {
        this.profileRepository = profileRepository;
        this.appUserRepository = appUserRepository;
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
            if (StringUtils.hasText(request.getDisplayName())) {
                p.setDisplayName(request.getDisplayName().trim());
            }
            // Completed when we have a display name
            p.setCompleted(StringUtils.hasText(p.getDisplayName()));
            return profileRepository.saveAndFlush(p);
        } else {
            Profile p = new Profile();
            p.setUser(user);
            String name = StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName().trim() : null;
            p.setDisplayName(name);
            // Completed when we have a display name
            p.setCompleted(StringUtils.hasText(name));
            // Verified flag will be mapped later from Firebase; default false
            return profileRepository.saveAndFlush(p);
        }
    }
}
