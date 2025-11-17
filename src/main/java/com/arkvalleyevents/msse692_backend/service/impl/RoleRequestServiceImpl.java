package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestCreateDto;
import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestDecisionDto;
import com.arkvalleyevents.msse692_backend.dto.response.RoleRequestDto;
import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import com.arkvalleyevents.msse692_backend.service.RoleRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleRequestServiceImpl implements RoleRequestService {

    private static final Logger log = LoggerFactory.getLogger(RoleRequestServiceImpl.class);
    private static final Set<String> ALLOWED_REQUESTABLE_ROLES = Set.of("EDITOR");

    @Override
    @Transactional
    public RoleRequestDto create(String requesterUid, RoleRequestCreateDto body) {
        requireNonBlank(requesterUid, "requesterUid");
        validateCreate(body);
        // TODO(step-6): persist role request entity (status=PENDING) and return mapped DTO
        log.info("role-request create: requesterUid={} roles={} reason={} (persistence pending)",
                requesterUid, safe(body.getRequestedRoles()), body.getReason());
        throw new UnsupportedOperationException("Not implemented yet (wired after Step 6: Flyway + repository)");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleRequestDto> listForUser(String requesterUid, Optional<RoleRequestStatus> status, Pageable pageable) {
        requireNonBlank(requesterUid, "requesterUid");
        if (pageable == null) {
            throw new IllegalArgumentException("pageable is required");
        }
        // TODO(step-6): query by requesterUid and optional status
        log.info("role-request listForUser: requesterUid={} status={} page={} (persistence pending)",
                requesterUid, status.orElse(null), pageable);
        return new PageImpl<>(List.of(), pageable, 0);
    }

    @Override
    @Transactional
    public RoleRequestDto cancel(String requesterUid, UUID id) {
        requireNonBlank(requesterUid, "requesterUid");
        requireNonNull(id, "id");
        // TODO(step-6): verify ownership + status=PENDING, then set CANCELLED
        log.info("role-request cancel: requesterUid={} id={} (persistence pending)", requesterUid, id);
        // Expected validations once entity is loaded:
        // - if not found: throw new EntityNotFoundException("Role request not found: " + id);
        // - if requester mismatch: throw new EntityNotFoundException to hide unauthorized
        // - if status != PENDING: throw new IllegalStateException("Only PENDING requests can be canceled");
        throw new UnsupportedOperationException("Not implemented yet (wired after Step 6: Flyway + repository)");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleRequestDto> adminList(Optional<RoleRequestStatus> status, Optional<String> search, Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("pageable is required");
        }
        // TODO(step-6): filter by status and search (uid/email) with pagination
        log.info("role-request adminList: status={} search={} page={} (persistence pending)", status.orElse(null), search.orElse(null), pageable);
        return new PageImpl<>(List.of(), pageable, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleRequestDto get(UUID id) {
        requireNonNull(id, "id");
        // TODO(step-6): find by id and map to DTO
        log.info("role-request get: id={} (persistence pending)", id);
        // Expected behavior once entity is loaded:
        // - if not found: throw new EntityNotFoundException("Role request not found: " + id);
        throw new UnsupportedOperationException("Not implemented yet (wired after Step 6: Flyway + repository)");
    }

    @Override
    @Transactional
    public RoleRequestDto approve(UUID id, String approverUid, RoleRequestDecisionDto body) {
        requireNonNull(id, "id");
        requireNonBlank(approverUid, "approverUid");
        // TODO(step-6): transition PENDING->APPROVED, record approver + note, apply roles, sync claims
        log.info("role-request approve: id={} approverUid={} note={} (persistence pending)", id, approverUid, safe(body.getApproverNote()));
        // Expected validations once entity is loaded:
        // - if not found: throw new EntityNotFoundException
        // - if status != PENDING: throw new IllegalStateException("Only PENDING requests can be approved");
        throw new UnsupportedOperationException("Not implemented yet (wired after Step 6: Flyway + repository)");
    }

    @Override
    @Transactional
    public RoleRequestDto reject(UUID id, String approverUid, RoleRequestDecisionDto body) {
        requireNonNull(id, "id");
        requireNonBlank(approverUid, "approverUid");
        // TODO(step-6): transition PENDING->REJECTED, record approver + note
        log.info("role-request reject: id={} approverUid={} note={} (persistence pending)", id, approverUid, safe(body.getApproverNote()));
        // Expected validations once entity is loaded:
        // - if not found: throw new EntityNotFoundException
        // - if status != PENDING: throw new IllegalStateException("Only PENDING requests can be rejected");
        throw new UnsupportedOperationException("Not implemented yet (wired after Step 6: Flyway + repository)");
    }

    private void validateCreate(RoleRequestCreateDto body) {
        if (body == null || body.getRequestedRoles() == null || body.getRequestedRoles().isEmpty()) {
            throw new IllegalArgumentException("requestedRoles is required");
        }
        boolean onlyAllowed = body.getRequestedRoles().stream()
                .filter(r -> r != null)
                .map(r -> r.trim().toUpperCase())
                .allMatch(ALLOWED_REQUESTABLE_ROLES::contains);
        if (!onlyAllowed) {
            throw new IllegalArgumentException("Only EDITOR role is requestable in Phase 1");
        }
    }

    private static Object safe(Object v) {
        return v;
    }

    private static void requireNonNull(Object v, String name) {
        if (v == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static void requireNonBlank(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
