package com.arkvalleyevents.msse692_backend.service;

public interface EventAuditService {
    void logCreate(Long eventId);
    void logUpdate(Long eventId);
    void logDelete(Long eventId);

    // Read-only retrieval (does not mutate DB)
    java.util.List<com.arkvalleyevents.msse692_backend.model.EventAudit> getRecentForEvent(Long eventId, int limit);
}
