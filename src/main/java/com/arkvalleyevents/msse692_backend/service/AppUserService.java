package com.arkvalleyevents.msse692_backend.service;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.dto.response.AppUserWithRolesDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.Set;

public interface AppUserService {
    AppUser getCurrentUser();
    Page<AppUserWithRolesDto> listUsers(Optional<String> q, Set<String> roles, Pageable pageable);
    AppUserWithRolesDto getByFirebaseUid(String firebaseUid);
}
