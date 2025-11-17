package com.arkvalleyevents.msse692_backend.service;

import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestCreateDto;
import com.arkvalleyevents.msse692_backend.dto.request.RoleRequestDecisionDto;
import com.arkvalleyevents.msse692_backend.dto.response.RoleRequestDto;
import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface RoleRequestService {
    RoleRequestDto create(String requesterUid, RoleRequestCreateDto body);

    Page<RoleRequestDto> listForUser(String requesterUid, Optional<RoleRequestStatus> status, Pageable pageable);

    RoleRequestDto cancel(String requesterUid, UUID id);

    Page<RoleRequestDto> adminList(Optional<RoleRequestStatus> status, Optional<String> search, Pageable pageable);

    RoleRequestDto get(UUID id);

    RoleRequestDto approve(UUID id, String approverUid, RoleRequestDecisionDto body);

    RoleRequestDto reject(UUID id, String approverUid, RoleRequestDecisionDto body);
}
