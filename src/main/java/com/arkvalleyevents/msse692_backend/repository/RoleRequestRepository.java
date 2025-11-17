package com.arkvalleyevents.msse692_backend.repository;

import com.arkvalleyevents.msse692_backend.model.RoleRequest;
import com.arkvalleyevents.msse692_backend.model.RoleRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRequestRepository extends JpaRepository<RoleRequest, String> {

    Page<RoleRequest> findByRequesterUid(String requesterUid, Pageable pageable);

    Page<RoleRequest> findByRequesterUidAndStatus(String requesterUid, RoleRequestStatus status, Pageable pageable);

    Page<RoleRequest> findByStatus(RoleRequestStatus status, Pageable pageable);

    boolean existsByRequesterUidAndStatus(String requesterUid, RoleRequestStatus status);
}
