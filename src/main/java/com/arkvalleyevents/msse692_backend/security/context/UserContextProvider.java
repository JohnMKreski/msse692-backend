package com.arkvalleyevents.msse692_backend.security.context;

import com.arkvalleyevents.msse692_backend.util.CurrentAuditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Provides the current user's context (id and role flags) from Spring Security.
 */
@Component
public class UserContextProvider {
    private static final Logger log = LoggerFactory.getLogger(UserContextProvider.class);

    public UserContext current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authPresent = auth != null;
        boolean authenticated = java.util.Optional.ofNullable(auth)
            .map(Authentication::isAuthenticated)
            .orElse(false);

        boolean admin = false;
        boolean editor = false;
        if (authenticated && auth != null) {
            admin = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .anyMatch(a -> "ROLE_ADMIN".equals(a));
            editor = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .anyMatch(a -> "ROLE_EDITOR".equals(a));
        }

        Long userId = CurrentAuditor.get().orElse(null);
        if (log.isDebugEnabled()) {
            log.debug("Resolved UserContext: userId={} admin={} editor={} (authPresent={} authenticated={})",
                    userId, admin, editor, authPresent, authenticated);
        }
        return new UserContext(userId, admin, editor);
    }
}
