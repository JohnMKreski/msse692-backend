package com.arkvalleyevents.msse692_backend.integration;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.model.AppUser;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.repository.AppUserRepository;
import com.arkvalleyevents.msse692_backend.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test hitting real H2 + Flyway schema for profile upsert behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=update"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ProfileIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private ProfileService profileService;

    private String firebaseUid = "it-firebase-001";

    @BeforeEach
    void seedUser() {
        AppUser u = new AppUser();
        u.setFirebaseUid(firebaseUid);
        u.setEmail("test@example.com");
        u.setDisplayName("Seed Name");
        appUserRepository.save(u);
    }

    @Test
    @Transactional
    void createProfile_thenUpdate_preservesIdAndTimestamps() {
        // Create
        ProfileRequest createReq = new ProfileRequest();
        createReq.setDisplayName("First Display");
        Profile created = profileService.upsertProfile(firebaseUid, createReq);

        assertNotNull(created.getId());
        assertEquals("First Display", created.getDisplayName());
        assertTrue(created.isCompleted());
        assertFalse(created.isVerified());
        assertNotNull(created.getCreatedAt());
        assertEquals(created.getCreatedAt(), created.getUpdatedAt(), "Update time should equal create time immediately after creation");

        // Update
        ProfileRequest updateReq = new ProfileRequest();
        updateReq.setDisplayName("Updated Display");
        Profile updated = profileService.upsertProfile(firebaseUid, updateReq);

        assertEquals(created.getId(), updated.getId(), "Profile id should remain constant across upsert");
        assertEquals("Updated Display", updated.getDisplayName());
        assertTrue(updated.getUpdatedAt().isAfter(updated.getCreatedAt()));
    }
}
