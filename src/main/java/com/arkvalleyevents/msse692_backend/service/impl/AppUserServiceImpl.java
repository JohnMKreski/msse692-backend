package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.AppUserService;
import com.arkvalleyevents.msse692_backend.dto.response.AppUserWithRolesDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository appUserRepository;
    private final UserContextProvider userContextProvider;

    public AppUserServiceImpl(AppUserRepository appUserRepository, UserContextProvider userContextProvider) {
        this.appUserRepository = appUserRepository;
        this.userContextProvider = userContextProvider;
    }

    @Override
    public AppUser getCurrentUser() {
        Long userId = userContextProvider.requireUserId();
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("No app user exists for this token."));
    }

    @Override
    public Page<AppUserWithRolesDto> listUsers(Optional<String> q, Set<String> roles, Pageable pageable) {
        String text = q.map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
        boolean rolesEmpty = (roles == null || roles.isEmpty());
        Page<AppUser> page;
        if (text == null && rolesEmpty) {
            page = appUserRepository.findAll(pageable);
        } else {
            page = appUserRepository.search(text, roles, rolesEmpty, pageable);
        }
        return page.map(this::toDto);
    }

    @Override
    public AppUserWithRolesDto getByFirebaseUid(String firebaseUid) {
        AppUser user = appUserRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + firebaseUid));
        return toDto(user);
    }

    private AppUserWithRolesDto toDto(AppUser u) {
        return new AppUserWithRolesDto(
                u.getId(),
                u.getFirebaseUid(),
                u.getEmail(),
                u.getDisplayName(),
                u.getPhotoUrl(),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                u.getRoles()
        );
    }
}
