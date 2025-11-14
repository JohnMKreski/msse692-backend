package com.arkvalleyevents.msse692_backend.security.context;

/**
 * Lightweight snapshot of the current authenticated user context.
 */
public record UserContext(Long userId, boolean admin, boolean editor) {
}
