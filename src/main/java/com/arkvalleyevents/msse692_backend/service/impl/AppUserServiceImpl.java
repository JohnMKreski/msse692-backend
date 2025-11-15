package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.AppUserService;
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
}
