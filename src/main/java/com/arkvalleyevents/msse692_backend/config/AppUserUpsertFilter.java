package com.arkvalleyevents.msse692_backend.config;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.repository.ProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * On authenticated requests, ensure an AppUser row exists keyed by Firebase UID
 * and upsert basic profile fields. Defaults role to USER on first create.
 */
@Component
public class AppUserUpsertFilter extends OncePerRequestFilter {

    private final AppUserRepository repository;
    private final ProfileRepository profileRepository;

    public AppUserUpsertFilter(AppUserRepository repository, ProfileRepository profileRepository) {
        this.repository = repository;
        this.profileRepository = profileRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwtPrincipal) {
            upsertFromJwt(jwtPrincipal);
        }
        filterChain.doFilter(request, response);
    }

    private void upsertFromJwt(Jwt jwt) {
        String uid = claim(jwt, "sub"); // Firebase UID is subject
        if (uid == null || uid.isBlank()) {
            uid = claim(jwt, "user_id");
        }
        if (uid == null || uid.isBlank()) return;

        Optional<AppUser> existing = repository.findByFirebaseUid(uid);
        if (existing.isPresent()) {
            AppUser u = existing.get();
            boolean changed = false;
            String email = claim(jwt, "email");
            String name = claim(jwt, "name");
            String picture = claim(jwt, "picture");

            if (email != null && !email.equals(u.getEmail())) { u.setEmail(email); changed = true; }

            // If a completed Profile exists for this user, do NOT overwrite displayName from JWT
            boolean hasCompletedProfile = false;
            try {
                hasCompletedProfile = profileRepository
                    .findByUserId(u.getId())
                    .map(Profile::isCompleted)
                    .orElse(false);
            } catch (Exception ignored) {
                // In case repository not available in certain profiles/tests, fail open to original behavior
            }

            if (!hasCompletedProfile) {
                if (name != null && !name.equals(u.getDisplayName())) { u.setDisplayName(name); changed = true; }
            }

            if (picture != null && !picture.equals(u.getPhotoUrl())) { u.setPhotoUrl(picture); changed = true; }
            if (changed) repository.save(Objects.requireNonNull(u));
        } else {
            AppUser u = new AppUser();
            u.setFirebaseUid(uid);
            u.setEmail(claim(jwt, "email"));
            u.setDisplayName(claim(jwt, "name"));
            u.setPhotoUrl(claim(jwt, "picture"));
            Set<String> roles = new HashSet<>();
            roles.add("USER");
            u.setRoles(roles);
            repository.save(u);
        }
    }

    private String claim(Jwt jwt, String name) {
        Object v = jwt.getClaims().get(name);
        return v != null ? String.valueOf(v) : null;
    }
}
