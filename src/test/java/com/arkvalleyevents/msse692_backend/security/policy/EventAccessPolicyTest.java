package com.arkvalleyevents.msse692_backend.security.policy;

import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EventAccessPolicyTest {

    private EventAccessPolicy policy;
    private EventDetailDto draftOwnedBy10;
    private EventDetailDto publishedOwnedBy20;

    @BeforeEach
    void setUp() {
        policy = new EventAccessPolicy();
        draftOwnedBy10 = new EventDetailDto();
        draftOwnedBy10.setEventId(1L);
        draftOwnedBy10.setStatus(EventStatus.DRAFT);
        draftOwnedBy10.setCreatedByUserId(10L);

        publishedOwnedBy20 = new EventDetailDto();
        publishedOwnedBy20.setEventId(2L);
        publishedOwnedBy20.setStatus(EventStatus.PUBLISHED);
        publishedOwnedBy20.setCreatedByUserId(20L);
    }

    @Test
    void admin_canView_anyEvent() {
        assertTrue(policy.canView(draftOwnedBy10, Optional.of(999L), true, false));
        assertTrue(policy.canView(publishedOwnedBy20, Optional.empty(), true, false));
    }

    @Test
    void editor_owner_canView_nonPublished() {
        assertTrue(policy.canView(draftOwnedBy10, Optional.of(10L), false, true));
    }

    @Test
    void editor_nonOwner_cannotView_nonPublished() {
        assertFalse(policy.canView(draftOwnedBy10, Optional.of(11L), false, true));
    }

    @Test
    void public_canView_only_published() {
        assertTrue(policy.canView(publishedOwnedBy20, Optional.empty(), false, false));
        assertFalse(policy.canView(draftOwnedBy10, Optional.empty(), false, false));
    }

    @Test
    void assertCanView_throws_when_denied() {
        assertThrows(EntityNotFoundException.class,
                () -> policy.assertCanView(draftOwnedBy10, Optional.of(11L), false, true));
    }

    @Test
    void assertCanView_allows_when_permitted() {
        assertDoesNotThrow(() -> policy.assertCanView(publishedOwnedBy20, Optional.empty(), false, false));
        assertDoesNotThrow(() -> policy.assertCanView(draftOwnedBy10, Optional.of(10L), false, true));
    }
}
