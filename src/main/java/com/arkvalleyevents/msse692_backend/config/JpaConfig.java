package com.arkvalleyevents.msse692_backend.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import com.arkvalleyevents.msse692_backend.util.CurrentAuditor;

/**
 * Enables JPA auditing and provides an AuditorAware<Long> that resolves the current user ID via CurrentAuditor (and optionally via a JWT claim).
 */

@Configuration
@EnableJpaAuditing(auditorAwareRef = "appUserAuditorAware")
public class JpaConfig {

	@Bean
	public AuditorAware<Long> appUserAuditorAware() {
		return new AuditorAware<Long>() {
			@Override
			@SuppressWarnings("null")
			public Optional<Long> getCurrentAuditor() {
				// First check thread-local set by AppUserUpsertFilter
				Optional<Long> tl = CurrentAuditor.get();
				if (tl.isPresent()) return tl;
				// Fallback: attempt to extract from SecurityContext without hitting repositories
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				if (auth == null || !auth.isAuthenticated()) return Optional.empty();
				Object principal = auth.getPrincipal();
				if (principal instanceof Jwt jwt) {
					String rawId = strClaim(jwt, "app_user_id"); // optional custom claim if added later
					if (rawId != null) {
						try { return Optional.of(Long.parseLong(rawId)); } catch (NumberFormatException ignored) {}
					}
				}
				return Optional.empty();
			}
		};
	}

	private static String strClaim(Jwt jwt, String name) {
		Object v = jwt.getClaims().get(name);
		return v != null ? String.valueOf(v) : null;
	}
}
