package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.model.EventAudit;
import com.arkvalleyevents.msse692_backend.repository.EventAuditRepository;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.service.EventAuditService;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventAuditServiceImpl implements EventAuditService {

    private final EventAuditRepository repository;
    private final AppUserRepository appUserRepository;

    public EventAuditServiceImpl(EventAuditRepository repository, AppUserRepository appUserRepository) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void logCreate(Long eventId) {
        save(eventId, "CREATE");
    }

    @Override
    public void logUpdate(Long eventId) {
        save(eventId, "UPDATE");
    }

    @Override
    public void logDelete(Long eventId) {
        save(eventId, "DELETE");
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<EventAudit> getRecentForEvent(Long eventId, int limit) {
        if (limit <= 0) limit = 10;
        if (limit <= 10) {
            return repository.findTop10ByEventIdOrderByAtDesc(eventId);
        } else {
            return repository.findTop50ByEventIdOrderByAtDesc(eventId).subList(0, Math.min(limit, 50));
        }
    }

    private void save(Long eventId, String action) {
        Long actorId = currentAppUserId().orElse(null);
        if (actorId == null) return; // don't audit if unauthenticated

        EventAudit ea = new EventAudit();
        ea.setEventId(eventId);
        ea.setActorUserId(actorId);
        ea.setAction(action);
        ea.setAt(OffsetDateTime.now());
        repository.save(ea);
    }

    private Optional<Long> currentAppUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        Object p = auth.getPrincipal();
        if (p instanceof Jwt jwt) {
            String uid = str(jwt, "sub");
            if (uid == null || uid.isBlank()) uid = str(jwt, "user_id");
            if (uid != null && !uid.isBlank()) {
                return appUserRepository.findByFirebaseUid(uid).map(u -> u.getId());
            }
        }
        return Optional.empty();
    }

    private static String str(Jwt jwt, String name) {
        Object v = jwt.getClaims().get(name);
        return v != null ? String.valueOf(v) : null;
    }
}
