package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestCreateDto;
import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestDecisionDto;
import com.arkvalleyevents.msse692_backend.dto.response.RoleRequestDto;
import com.arkvalleyevents.msse692_backend.model.RoleRequest;
import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import com.arkvalleyevents.msse692_backend.repository.RoleRequestRepository;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import com.arkvalleyevents.msse692_backend.service.RoleRequestService;
import com.arkvalleyevents.msse692_backend.service.UserRoleService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoleRequestServiceImpl implements RoleRequestService {

    private static final Logger log = LoggerFactory.getLogger(RoleRequestServiceImpl.class);
    private static final Set<String> ALLOWED_REQUESTABLE_ROLES = Set.of("EDITOR");

    private final RoleRequestRepository repository;
    private final UserRoleService userRoleService;
    private final UserContextProvider userContextProvider;

    public RoleRequestServiceImpl(RoleRequestRepository repository,
                                  UserRoleService userRoleService,
                                  UserContextProvider userContextProvider) {
        this.repository = repository;
        this.userRoleService = userRoleService;
        this.userContextProvider = userContextProvider;
    }

    @Override
    @Transactional
    public RoleRequestDto create(String requesterUid, RoleRequestCreateDto body) {
        requireNonBlank(requesterUid, "requesterUid");
        validateCreate(body);

        // Guard: only one PENDING per requester (service-level; DB partial index can come later)
        boolean alreadyPending = repository.existsByRequesterUidAndStatus(requesterUid, RoleRequestStatus.PENDING);
        if (alreadyPending) {
            throw new IllegalStateException("Existing PENDING role request for requesterUid=" + requesterUid);
        }

        RoleRequest entity = new RoleRequest();
        entity.setRequesterUid(requesterUid);
        entity.getRequestedRoles().addAll(normalizeRoles(body.getRequestedRoles()));
        entity.setReason(trimToNull(body.getReason()));
        entity.setStatus(RoleRequestStatus.PENDING); // explicit for clarity
        // id + createdAt via @PrePersist

        RoleRequest saved = repository.save(entity);
        Long actorId = userContextProvider.current().userId();
        log.info("role-request create: actorId={} requesterUid={} roles={} reason={}", actorId, requesterUid, saved.getRequestedRoles(), saved.getReason());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleRequestDto> listForUser(String requesterUid, Optional<RoleRequestStatus> status, Pageable pageable) {
        requireNonBlank(requesterUid, "requesterUid");
        if (pageable == null) throw new IllegalArgumentException("pageable is required");
        Page<RoleRequest> page = status
                .map(s -> repository.findByRequesterUidAndStatus(requesterUid, s, pageable))
                .orElseGet(() -> repository.findByRequesterUid(requesterUid, pageable));
        List<RoleRequestDto> content = page.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    @Transactional
    public RoleRequestDto cancel(String requesterUid, UUID id) {
        requireNonBlank(requesterUid, "requesterUid");
        requireNonNull(id, "id");
        RoleRequest entity = repository.findById(id.toString())
                .orElseThrow(() -> new EntityNotFoundException("Role request not found: " + id));
        if (!entity.getRequesterUid().equals(requesterUid)) {
            // Hide existence to requester mismatch
            throw new EntityNotFoundException("Role request not found: " + id);
        }
        if (entity.getStatus() != RoleRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be canceled");
        }
        entity.setStatus(RoleRequestStatus.CANCELED);
        entity.setDecidedAt(OffsetDateTime.now());
        RoleRequest saved = repository.save(entity);
        Long actorId = userContextProvider.current().userId();
        log.info("role-request cancel: actorId={} requesterUid={} id={} status={}", actorId, requesterUid, id, saved.getStatus());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleRequestDto> adminList(Optional<RoleRequestStatus> status, Optional<String> search, Pageable pageable) {
        if (pageable == null) throw new IllegalArgumentException("pageable is required");
        // Simple filtering permutations (exact match on requesterUid if search provided)
        Page<RoleRequest> page;
        if (search.isPresent() && status.isPresent()) {
            page = repository.findByRequesterUidAndStatus(search.get(), status.get(), pageable);
        } else if (search.isPresent()) {
            page = repository.findByRequesterUid(search.get(), pageable);
        } else if (status.isPresent()) {
            page = repository.findByStatus(status.get(), pageable);
        } else {
            page = repository.findAll(pageable);
        }
        List<RoleRequestDto> content = page.getContent().stream().map(this::toDto).collect(Collectors.toList());
        Long actorId = userContextProvider.current().userId();
        log.info("role-request adminList: actorId={} status={} search={} returned={} page={} total={}", actorId, status.orElse(null), search.orElse(null), content.size(), pageable, page.getTotalElements());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleRequestDto get(UUID id) {
        requireNonNull(id, "id");
        RoleRequest entity = repository.findById(id.toString())
                .orElseThrow(() -> new EntityNotFoundException("Role request not found: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional
    public RoleRequestDto approve(UUID id, String approverUid, RoleRequestDecisionDto body) {
        requireNonNull(id, "id");
        requireNonBlank(approverUid, "approverUid");
        RoleRequest entity = repository.findById(id.toString())
                .orElseThrow(() -> new EntityNotFoundException("Role request not found: " + id));
        if (entity.getStatus() != RoleRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved");
        }
        entity.setStatus(RoleRequestStatus.APPROVED);
        entity.setApproverUid(approverUid);
        entity.setApproverNote(trimToNull(body != null ? body.getApproverNote() : null));
        entity.setDecidedAt(OffsetDateTime.now());
        RoleRequest saved = repository.save(entity);

        // Apply roles to user and sync claims via existing service
        userRoleService.addRoles(saved.getRequesterUid(), saved.getRequestedRoles());
        // userRoleService itself logs and performs claims sync; we only log decision here
        Long actorId = userContextProvider.current().userId();
        log.info("role-request approve: actorId={} id={} requesterUid={} roles={}", actorId, id, saved.getRequesterUid(), saved.getRequestedRoles());
        return toDto(saved);
    }

    @Override
    @Transactional
    public RoleRequestDto reject(UUID id, String approverUid, RoleRequestDecisionDto body) {
        requireNonNull(id, "id");
        requireNonBlank(approverUid, "approverUid");
        RoleRequest entity = repository.findById(id.toString())
                .orElseThrow(() -> new EntityNotFoundException("Role request not found: " + id));
        if (entity.getStatus() != RoleRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected");
        }
        entity.setStatus(RoleRequestStatus.REJECTED);
        entity.setApproverUid(approverUid);
        entity.setApproverNote(trimToNull(body != null ? body.getApproverNote() : null));
        entity.setDecidedAt(OffsetDateTime.now());
        RoleRequest saved = repository.save(entity);
        Long actorId = userContextProvider.current().userId();
        log.info("role-request reject: actorId={} id={} requesterUid={} noteLength={}",
                actorId, id, saved.getRequesterUid(), saved.getApproverNote() != null ? saved.getApproverNote().length() : 0);
        return toDto(saved);
    }

    private void validateCreate(RoleRequestCreateDto body) {
        if (body == null || body.getRequestedRoles() == null || body.getRequestedRoles().isEmpty()) {
            throw new IllegalArgumentException("requestedRoles is required");
        }
        boolean onlyAllowed = body.getRequestedRoles().stream()
                .filter(r -> r != null)
                .map(r -> r.trim().toUpperCase(Locale.ROOT))
                .allMatch(ALLOWED_REQUESTABLE_ROLES::contains);
        if (!onlyAllowed) {
            throw new IllegalArgumentException("Only EDITOR role is requestable in Phase 1");
        }
    }

    private Set<String> normalizeRoles(List<String> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .filter(r -> r != null)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private RoleRequestDto toDto(RoleRequest entity) {
        RoleRequestDto dto = new RoleRequestDto();
        dto.setId(entity.getId());
        dto.setRequesterUid(entity.getRequesterUid());
        // stable ordering for deterministic tests / UI
        List<String> sortedRoles = entity.getRequestedRoles() == null ? new ArrayList<>() : entity.getRequestedRoles().stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        dto.setRequestedRoles(sortedRoles);
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        dto.setApproverUid(entity.getApproverUid());
        dto.setApproverNote(entity.getApproverNote());
        dto.setDecidedAt(entity.getDecidedAt() != null ? entity.getDecidedAt().toInstant() : null);
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null);
        return dto;
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
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
