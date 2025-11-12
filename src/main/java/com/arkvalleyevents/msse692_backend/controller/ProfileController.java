package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.dto.response.ProfileResponse;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.service.ProfileService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile") // API versioned base path (added v1)
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me") // GET /api/v1/profile/me
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        String uid = extractUid(authentication);
        return profileService.getCurrentProfile(uid)
            .<ResponseEntity<?>>map(p -> ResponseEntity.ok(toResponse(p)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("PROFILE_NOT_FOUND", "No profile exists for this user")));
    }

    @PostMapping // POST /api/v1/profile
    public ResponseEntity<?> upsertMyProfile(Authentication authentication, @Valid @RequestBody ProfileRequest request) {
        String uid = extractUid(authentication);
        boolean existed = profileService.getCurrentProfile(uid).isPresent();
        Profile saved = profileService.upsertProfile(uid, request);
        ProfileResponse body = toResponse(saved);
        if (existed) {
            return ResponseEntity.ok(body);
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
    }

    private String extractUid(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = strClaim(jwt, "sub");
            if (sub != null && !sub.isBlank()) return sub;
            String userId = strClaim(jwt, "user_id");
            if (userId != null && !userId.isBlank()) return userId;
        }
        throw new IllegalStateException("UNAUTHENTICATED");
    }

    private String strClaim(Jwt jwt, String name) {
        Object v = jwt.getClaims().get(name);
        return v != null ? String.valueOf(v) : null;
    }

    private ProfileResponse toResponse(Profile p) {
        ProfileResponse r = new ProfileResponse();
        r.setId(p.getId());
        r.setUserId(p.getUser() != null ? p.getUser().getId() : null);
        r.setDisplayName(p.getDisplayName());
        r.setCompleted(p.isCompleted());
        r.setVerified(p.isVerified());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }

    private static ErrorBody error(String code, String message) {
        return new ErrorBody(code, message, OffsetDateTime.now());
    }

    public record ErrorBody(String code, String message, OffsetDateTime timestamp) {}
}
