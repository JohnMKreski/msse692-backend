package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import com.arkvalleyevents.msse692_backend.repository.RoleRequestRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.arkvalleyevents.msse692_backend.config.SecurityConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.when;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(SecurityConfig.class)
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://test-issuer"
})
class RoleRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRequestRepository repository;

    private final String requesterUid = "test-user-123";
    @MockitoBean
    private JwtDecoder jwtDecoder; // mock out network call for issuer discovery
    @MockitoBean
    private UserContextProvider userContextProvider; // mock user context provider

    @BeforeEach
    void clean() {
        repository.deleteAll();
        when(userContextProvider.current()).thenReturn(new UserContext(777L, true, true));
    }

    @Test
    void createRequest_success_returns200_andPersists() throws Exception {
        mockMvc.perform(post("/api/roles/requests")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", requesterUid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestedRoles\":[\"editor\"],\"reason\":\"Help with events\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.requesterUid").value(requesterUid))
            .andExpect(jsonPath("$.requestedRoles[0]").value("EDITOR"))
            .andExpect(jsonPath("$.status").value("Pending"));

        // Verify persisted entity
        assert repository.count() == 1;
        var entity = repository.findAll().get(0);
        assert entity.getRequesterUid().equals(requesterUid);
        assert entity.getStatus() == RoleRequestStatus.PENDING;
    }

    @Test
    void createRequest_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/roles/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestedRoles\":[\"editor\"]}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createRequest_duplicatePending_returns409IllegalState() throws Exception {
        // First create
        mockMvc.perform(post("/api/roles/requests")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", requesterUid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestedRoles\":[\"editor\"]}"))
            .andExpect(status().isOk());

        // Second create should conflict
        mockMvc.perform(post("/api/roles/requests")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", requesterUid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestedRoles\":[\"editor\"]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ILLEGAL_STATE"))
            .andExpect(jsonPath("$.message", containsString("Existing PENDING")));
    }

    @Test
    void listRequests_returnsPageWithCreated() throws Exception {
        // create one then list
        mockMvc.perform(post("/api/roles/requests")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", requesterUid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestedRoles\":[\"editor\"],\"reason\":\"Help\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/roles/requests")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", requesterUid)))
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].requestedRoles[0]").value("EDITOR"))
            .andExpect(jsonPath("$.content[0].status").value("Pending"));
    }

    @Test
    void cancelRequest_transitionsToCanceled() throws Exception {
        // create
        String id = mockMvc.perform(post("/api/roles/requests")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", requesterUid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestedRoles\":[\"editor\"]}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([a-f0-9-]+)\".*", "$1");

        // cancel
        mockMvc.perform(post("/api/roles/requests/{id}/cancel", UUID.fromString(id))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", requesterUid))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Canceled"));

        var entity = repository.findById(id).orElseThrow();
        assert entity.getStatus() == RoleRequestStatus.CANCELED;
    }

    @Test
    void createRequest_invalidRole_returns400() throws Exception {
        mockMvc.perform(post("/api/roles/requests")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .jwt(j -> j.claim("sub", requesterUid)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestedRoles\":[\"admin\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
            .andExpect(jsonPath("$.message", containsString("Only EDITOR")));
    }

        @Test
        void cancelRequest_nonPending_returns409() throws Exception {
        // Seed an APPROVED request directly
        var approved = new com.arkvalleyevents.msse692_backend.model.RoleRequest();
        approved.setRequesterUid(requesterUid);
        approved.getRequestedRoles().add("EDITOR");
        approved.setStatus(RoleRequestStatus.APPROVED);
        repository.save(approved);

        mockMvc.perform(post("/api/roles/requests/{id}/cancel", UUID.fromString(approved.getId()))
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .jwt(j -> j.claim("sub", requesterUid))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ILLEGAL_STATE"))
            .andExpect(jsonPath("$.message", containsString("Only PENDING")));
        }

        @Test
        void cancelRequest_wrongRequester_returns404() throws Exception {
        // Create request for different user
        String otherUid = "other-user-999";
        String id = mockMvc.perform(post("/api/roles/requests")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .jwt(j -> j.claim("sub", otherUid)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"requestedRoles\":[\"editor\"]}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"id\":\"([a-f0-9-]+)\".*", "$1");

        // Attempt cancel with different requester principal
        mockMvc.perform(post("/api/roles/requests/{id}/cancel", UUID.fromString(id))
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .jwt(j -> j.claim("sub", requesterUid))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        void cancelRequest_malformedUuid_returns400TypeMismatch() throws Exception {
        mockMvc.perform(post("/api/roles/requests/{id}/cancel", "not-a-uuid")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .jwt(j -> j.claim("sub", requesterUid))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));
        }
}
