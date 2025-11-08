package com.arkvalleyevents.msse692_backend.repository;

import com.arkvalleyevents.msse692_backend.model.EventAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventAuditRepository extends JpaRepository<EventAudit, Long> {
	java.util.List<EventAudit> findTop10ByEventIdOrderByAtDesc(Long eventId);
	java.util.List<EventAudit> findTop50ByEventIdOrderByAtDesc(Long eventId);
}
