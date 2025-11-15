package com.arkvalleyevents.msse692_backend.security.policy;

import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventListPolicyTest {

    private final EventListPolicy policy = new EventListPolicy();

    @Test
    void admin_noDefaultsApplied() {
        Map<String, String> in = new HashMap<>();
        UserContext admin = new UserContext(1L, true, false);
        Map<String, String> out = policy.applyListDefaults(in, admin);
        assertFalse(out.containsKey("status"));
        assertFalse(out.containsKey("createdByUserId"));
        assertFalse(out.containsKey("ownerOrPublished"));
    }

    @Test
    void editor_defaultsOwnerAndPublished() {
        Map<String, String> in = new HashMap<>();
        UserContext editor = new UserContext(10L, false, true);
        Map<String, String> out = policy.applyListDefaults(in, editor);
        assertEquals("10", out.get("createdByUserId"));
        assertEquals("true", out.get("ownerOrPublished"));
        assertFalse(out.containsKey("status"));
    }

    @Test
    void editor_withExplicitStatus_keepsStatus_noOwnerOrPublishedOverride() {
        Map<String, String> in = new HashMap<>();
        in.put("status", "UNPUBLISHED");
        UserContext editor = new UserContext(10L, false, true);
        Map<String, String> out = policy.applyListDefaults(in, editor);
        assertEquals("10", out.get("createdByUserId"));
        assertEquals("UNPUBLISHED", out.get("status"));
        // ownerOrPublished not required if status explicitly provided
        assertNull(out.get("ownerOrPublished"));
    }

    @Test
    void public_defaultsPublished() {
        Map<String, String> in = new HashMap<>();
        UserContext user = new UserContext(null, false, false);
        Map<String, String> out = policy.applyListDefaults(in, user);
        assertEquals("PUBLISHED", out.get("status"));
        assertFalse(out.containsKey("createdByUserId"));
    }

    @Test
    void editor_withoutUserId_constrainsToImpossibleOwner() {
        Map<String, String> in = new HashMap<>();
        UserContext editor = new UserContext(null, false, true);
        Map<String, String> out = policy.applyListDefaults(in, editor);
        assertEquals("-1", out.get("createdByUserId"));
    }
}
