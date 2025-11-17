package com.arkvalleyevents.msse692_backend.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a user role elevation request (Phase 1).
 * Transitions (service layer enforced):
 * PENDING -> APPROVED | REJECTED | CANCELED
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Table(name = "role_requests")
public class RoleRequest {

    @Id
    // Explicitly generate UUID in code (portable across DBs)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id; // store UUID as string (consistent across engines)

    @Column(name = "requester_uid", nullable = false, length = 128)
    @ToString.Include
    private String requesterUid;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_request_roles", joinColumns = @JoinColumn(name = "role_request_id"))
    @Column(name = "role", length = 50, nullable = false)
    private Set<String> requestedRoles = new HashSet<>();

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ToString.Include
    private RoleRequestStatus status = RoleRequestStatus.PENDING;

    @Column(name = "approver_uid", length = 128)
    private String approverUid;

    @Column(name = "approver_note", length = 500)
    private String approverNote;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = RoleRequestStatus.PENDING;
    }
}
