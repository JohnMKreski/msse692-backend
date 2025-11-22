package com.arkvalleyevents.msse692_backend.dto.response;

import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
public class RoleRequestDto {
    private String id;
    private String requesterUid;
    private List<String> requestedRoles;
    private String reason;

    private RoleRequestStatus status;
    private String approverUid;
    private String approverNote;
    private Instant decidedAt;

    private Instant createdAt;
}
