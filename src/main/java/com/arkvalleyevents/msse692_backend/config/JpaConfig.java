package com.arkvalleyevents.msse692_backend.config;

import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "appUserAuditorAware")
public class JpaConfig {

	@Bean
	public AuditorAware<Long> appUserAuditorAware(AppUserRepository appUserRepository) {
		return new AuditorAware<Long>() {
			@Override
			@SuppressWarnings("null")
			public Optional<Long> getCurrentAuditor() {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				if (auth == null || !auth.isAuthenticated()) {
					return Optional.empty();
				}
				Object principal = auth.getPrincipal();
				if (principal instanceof Jwt jwt) {
					String uid = strClaim(jwt, "sub");
					if (uid == null || uid.isBlank()) uid = strClaim(jwt, "user_id");
					if (uid != null && !uid.isBlank()) {
						return appUserRepository.findByFirebaseUid(uid).map(AppUser::getId);
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
