package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app-users") // API versioned base path (added v1)
public class AppUserController {

    private final AppUserRepository repository;

    public AppUserController(AppUserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me") // GET /api/v1/app-users/me
    public ResponseEntity<?> me(Authentication authentication) {
        String uid = extractUid(authentication);
        Optional<AppUser> user = repository.findByFirebaseUid(uid);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorBody("APP_USER_NOT_FOUND", "No app user exists for this token.", OffsetDateTime.now()));
    }

    private String extractUid(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            Object sub = jwt.getClaims().get("sub");
            if (sub != null && !String.valueOf(sub).isBlank()) return String.valueOf(sub);
            Object userId = jwt.getClaims().get("user_id");
            if (userId != null && !String.valueOf(userId).isBlank()) return String.valueOf(userId);
        }
        throw new IllegalStateException("UNAUTHENTICATED");
    }

    public record ErrorBody(String code, String message, OffsetDateTime timestamp) {}
}
