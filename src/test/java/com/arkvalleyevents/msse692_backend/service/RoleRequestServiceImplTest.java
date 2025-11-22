package com.arkvalleyevents.msse692_backend.service;

import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestCreateDto;
import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestDecisionDto;
import com.arkvalleyevents.msse692_backend.dto.response.RoleRequestDto;
import com.arkvalleyevents.msse692_backend.model.RoleRequest;
import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import com.arkvalleyevents.msse692_backend.repository.RoleRequestRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.impl.RoleRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleRequestServiceImplTest {

    @Mock
    private RoleRequestRepository repository;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private UserContextProvider userContextProvider;

    @InjectMocks
    private RoleRequestServiceImpl service;

    @BeforeEach
    void setup() {
        // lenient because some negative tests fail before auditing (which calls current())
        lenient().when(userContextProvider.current()).thenReturn(new UserContext(99L, true, true));
    }

    @Test
    void create_success_normalizesRolesAndPersists() {
        RoleRequestCreateDto body = new RoleRequestCreateDto();
        body.setRequestedRoles(List.of("editor", "EDITOR")); // duplicates + case variance
        body.setReason("I would like to help");

        when(repository.existsByRequesterUidAndStatus("uid123", RoleRequestStatus.PENDING)).thenReturn(false);
        when(repository.save(any(RoleRequest.class))).thenAnswer(invocation -> {
            RoleRequest r = invocation.getArgument(0);
            r.setId(UUID.randomUUID().toString());
            r.setCreatedAt(OffsetDateTime.now());
            r.setVersion(0L);
            return r;
        });

        RoleRequestDto dto = service.create("uid123", body);

        assertNotNull(dto.getId());
        assertEquals("uid123", dto.getRequesterUid());
        assertEquals(List.of("EDITOR"), dto.getRequestedRoles(), "Roles should be de-duped and uppercased, sorted");
        assertEquals(RoleRequestStatus.PENDING, dto.getStatus());

        ArgumentCaptor<RoleRequest> captor = ArgumentCaptor.forClass(RoleRequest.class);
        verify(repository).save(captor.capture());
        RoleRequest saved = captor.getValue();
        assertTrue(saved.getRequestedRoles().contains("EDITOR"));
        assertEquals(1, saved.getRequestedRoles().size());
    }

    @Test
    void create_duplicatePendingThrowsIllegalState() {
        RoleRequestCreateDto body = new RoleRequestCreateDto();
        body.setRequestedRoles(List.of("EDITOR"));
        when(repository.existsByRequesterUidAndStatus("uid123", RoleRequestStatus.PENDING)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.create("uid123", body));
        assertTrue(ex.getMessage().contains("Existing PENDING"));
        verify(repository, never()).save(any());
    }

    @Test
    void create_emptyRolesThrowsIllegalArgument() {
        RoleRequestCreateDto body = new RoleRequestCreateDto();
        body.setRequestedRoles(List.of());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create("uid123", body));
        assertTrue(ex.getMessage().contains("requestedRoles is required"));
    }

    @Test
    void create_disallowedRoleThrowsIllegalArgument() {
        RoleRequestCreateDto body = new RoleRequestCreateDto();
        body.setRequestedRoles(List.of("ADMIN"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create("uid123", body));
        assertTrue(ex.getMessage().contains("Only EDITOR role"));
    }

    @Test
    void approve_transitionsAndCallsUserRoleService() {
        UUID id = UUID.randomUUID();
        RoleRequest existing = new RoleRequest();
        existing.setId(id.toString());
        existing.setRequesterUid("uid999");
        existing.setStatus(RoleRequestStatus.PENDING);
        existing.getRequestedRoles().add("EDITOR");
        existing.setCreatedAt(OffsetDateTime.now().minusMinutes(5));
        existing.setVersion(0L);

        when(repository.findById(id.toString())).thenReturn(Optional.of(existing));
        when(repository.save(any(RoleRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleRequestDecisionDto decision = new RoleRequestDecisionDto();
        decision.setApproverNote("Looks good");
        RoleRequestDto dto = service.approve(id, "adminUID", decision);

        assertEquals(RoleRequestStatus.APPROVED, dto.getStatus());
        verify(userRoleService).addRoles("uid999", Set.of("EDITOR"));
    }

    @Test
    void approve_wrongStatusThrowsIllegalState() {
        UUID id = UUID.randomUUID();
        RoleRequest existing = new RoleRequest();
        existing.setId(id.toString());
        existing.setRequesterUid("uid999");
        existing.setStatus(RoleRequestStatus.APPROVED); // already approved
        existing.getRequestedRoles().add("EDITOR");

        when(repository.findById(id.toString())).thenReturn(Optional.of(existing));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.approve(id, "adminUID", new RoleRequestDecisionDto()));
        assertTrue(ex.getMessage().contains("Only PENDING"));
        verify(repository, never()).save(any());
    }

    @Test
    void cancel_success() {
        UUID id = UUID.randomUUID();
        RoleRequest existing = new RoleRequest();
        existing.setId(id.toString());
        existing.setRequesterUid("userABC");
        existing.setStatus(RoleRequestStatus.PENDING);
        existing.getRequestedRoles().add("EDITOR");

        when(repository.findById(id.toString())).thenReturn(Optional.of(existing));
        when(repository.save(any(RoleRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleRequestDto dto = service.cancel("userABC", id);
        assertEquals(RoleRequestStatus.CANCELED, dto.getStatus());
    }

    @Test
    void reject_success() {
        UUID id = UUID.randomUUID();
        RoleRequest existing = new RoleRequest();
        existing.setId(id.toString());
        existing.setRequesterUid("userABC");
        existing.setStatus(RoleRequestStatus.PENDING);
        existing.getRequestedRoles().add("EDITOR");

        when(repository.findById(id.toString())).thenReturn(Optional.of(existing));
        when(repository.save(any(RoleRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleRequestDecisionDto decision = new RoleRequestDecisionDto();
        decision.setApproverNote("Not enough info");
        RoleRequestDto dto = service.reject(id, "adminUID", decision);
        assertEquals(RoleRequestStatus.REJECTED, dto.getStatus());
        assertEquals("adminUID", dto.getApproverUid());
    }
}
