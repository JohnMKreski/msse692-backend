package com.arkvalleyevents.msse692_backend.security.policy;

import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies role-aware default filters for event list queries.
 * - ADMIN: no defaults
 * - EDITOR: own events OR published (when status not explicitly provided)
 * - Public/USER: status=PUBLISHED (when not explicitly provided)
 */
@Component
public class EventListPolicy {
    private static final Logger log = LoggerFactory.getLogger(EventListPolicy.class);

    public Map<String, String> applyListDefaults(Map<String, String> inputFilters, UserContext uc) {
        Map<String, String> out = new HashMap<>(inputFilters == null ? Map.of() : inputFilters);
        if (uc == null) {
            // Treat as anonymous
            out.putIfAbsent("status", EventStatus.PUBLISHED.name());
            log.debug("ListPolicy: anonymous → status=PUBLISHED (defaults applied)");
            return out;
        }
        if (uc.admin()) {
            log.debug("ListPolicy: admin=true → no defaults applied");
            return out;
        }
        if (uc.editor()) {
            Long uid = uc.userId();
            if (uid != null) {
                out.put("createdByUserId", String.valueOf(uid));
                if (!out.containsKey("status")) {
                    out.put("ownerOrPublished", "true");
                }
                log.debug("ListPolicy: editor userId={} → owner + published defaults applied", uid);
            } else {
                // No user id → constrain to impossible owner to avoid leaking data
                out.put("createdByUserId", "-1");
                log.debug("ListPolicy: editor without userId → constrain to createdBy=-1");
            }
            return out;
        }
        // Anonymous/USER
        out.putIfAbsent("status", EventStatus.PUBLISHED.name());
        log.debug("ListPolicy: user/public → status=PUBLISHED (defaults applied)");
        return out;
    }
}
