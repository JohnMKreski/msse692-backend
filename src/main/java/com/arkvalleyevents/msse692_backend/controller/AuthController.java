package com.arkvalleyevents.msse692_backend.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth") // API versioned base path (added v1)
public class AuthController {

    @GetMapping("/whoami") // GET /api/v1/auth/whoami
    public Map<String, Object> whoAmI(Authentication auth) {
        Map<String, Object> out = new HashMap<>();
        if (auth == null) {
            out.put("authenticated", false);
            return out;
        }
        out.put("authenticated", auth.isAuthenticated());
        out.put("name", auth.getName());
        if (auth.getPrincipal() instanceof Jwt jwt) {
            out.put("subject", jwt.getSubject());
            out.put("claims", jwt.getClaims());
        }
        out.put("authorities", auth.getAuthorities());
        return out;
    }
}
