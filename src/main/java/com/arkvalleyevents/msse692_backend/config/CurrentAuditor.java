package com.arkvalleyevents.msse692_backend.config;

import java.util.Optional;

/**
 * Holds the current AppUser ID for the lifetime of a request/thread so that
 * AuditorAware can resolve @CreatedBy/@LastModifiedBy without hitting the database.
 */
public final class CurrentAuditor {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private CurrentAuditor() {}

    public static void set(Long id) {
        if (id != null) CURRENT.set(id);
    }

    public static Optional<Long> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
