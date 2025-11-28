package com.arkvalleyevents.msse692_backend.service.mapping;

import com.arkvalleyevents.msse692_backend.dto.request.ProfileRequest;
import com.arkvalleyevents.msse692_backend.dto.response.ProfileResponse;
import com.arkvalleyevents.msse692_backend.model.Profile;
import com.arkvalleyevents.msse692_backend.model.ProfileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ProfileMapperTest {

    // Use generated MapStruct implementation directly
    private final ProfileMapper mapper = new ProfileMapperImpl();

    @Test
    @DisplayName("toEntity maps lists to JSON strings")
    void toEntity_mapsLists() throws Exception {
        ProfileRequest req = buildRequest("Venue Name", ProfileType.VENUE,
                "Some Location", "Desc", List.of("fb","ig"), List.of("https://site"));

        Profile entity = mapper.toEntity(req);

        assertEquals("Venue Name", entity.getDisplayName());
        assertEquals(ProfileType.VENUE, entity.getProfileType());
        assertEquals("Some Location", entity.getLocation());
        assertEquals("Desc", entity.getDescription());
        assertEquals("[\"fb\",\"ig\"]", entity.getSocials());
        assertEquals("[\"https://site\"]", entity.getWebsites());
    }

    @Test
    @DisplayName("toResponse maps JSON strings back to lists")
    void toResponse_mapsJsonToLists() {
        Profile entity = new Profile();
        entity.setDisplayName("Artist");
        entity.setProfileType(ProfileType.ARTIST);
        entity.setLocation("Town");
        entity.setDescription("Bio");
        entity.setSocials("[\"x\",\"y\",null]");
        entity.setWebsites("[]");

        ProfileResponse resp = mapper.toResponse(entity);

        assertEquals("Artist", resp.getDisplayName());
        assertEquals(ProfileType.ARTIST, resp.getProfileType());
        assertEquals("Town", resp.getLocation());
        assertEquals("Bio", resp.getDescription());
        assertEquals(Arrays.asList("x","y", null), resp.getSocials());
        assertEquals(List.of(), resp.getWebsites());
    }

    @Test
    @DisplayName("updateEntity ignores null list fields (null-ignore partial update)")
    void updateEntity_nullListsIgnored() throws Exception {
        Profile existing = new Profile();
        existing.setDisplayName("Orig");
        existing.setProfileType(ProfileType.OTHER);
        existing.setSocials("[\"keep\"]");
        existing.setWebsites("[\"oldsite\"]");

        // Request with displayName change but null socials/websites (should not overwrite)
        ProfileRequest patchReq = buildRequest("New Name", ProfileType.OTHER, null, null, null, null);

        mapper.updateEntity(existing, patchReq);

        assertEquals("New Name", existing.getDisplayName());
        assertEquals("[\"keep\"]", existing.getSocials(), "Existing socials should remain");
        assertEquals("[\"oldsite\"]", existing.getWebsites(), "Existing websites should remain");
    }

    @Test
    @DisplayName("updateEntity overwrites lists when provided")
    void updateEntity_overwritesLists() throws Exception {
        Profile existing = new Profile();
        existing.setDisplayName("Orig");
        existing.setProfileType(ProfileType.OTHER);
        existing.setSocials("[\"keep\"]");
        existing.setWebsites("[\"oldsite\"]");

        ProfileRequest patchReq = buildRequest(null, ProfileType.OTHER, null, null,
                List.of("new1","new2"), List.of("https://new"));

        mapper.updateEntity(existing, patchReq);

        assertEquals("[\"new1\",\"new2\"]", existing.getSocials());
        assertEquals("[\"https://new\"]", existing.getWebsites());
        // Display name null in request -> ignored; stays Orig
        assertEquals("Orig", existing.getDisplayName());
    }

    @Test
    @DisplayName("toResponse with null JSON fields returns null lists")
    void toResponse_nullJson() {
        Profile entity = new Profile();
        entity.setDisplayName("None");
        entity.setProfileType(ProfileType.OTHER);
        entity.setSocials(null);
        entity.setWebsites(null);

        ProfileResponse resp = mapper.toResponse(entity);
        assertNull(resp.getSocials());
        assertNull(resp.getWebsites());
    }

    // Helper to build request via reflection (request has only getters)
    private ProfileRequest buildRequest(String displayName, ProfileType type, String location, String description,
                                        List<String> socials, List<String> websites) throws Exception {
        ProfileRequest req = new ProfileRequest();
        set(req, "displayName", displayName);
        set(req, "profileType", type);
        set(req, "location", location);
        set(req, "description", description);
        set(req, "socials", socials);
        set(req, "websites", websites);
        return req;
    }

    private void set(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
