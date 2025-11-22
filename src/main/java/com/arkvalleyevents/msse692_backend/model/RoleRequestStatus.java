package com.arkvalleyevents.msse692_backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoleRequestStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELED("Canceled");

    private final String displayName;

    RoleRequestStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static RoleRequestStatus fromString(String s) {
        if (s == null) return null;
        String v = s.trim();
        String u = v.toUpperCase();
        switch (u) {
            case "PENDING": return PENDING;
            case "APPROVED": return APPROVED;
            case "REJECTED": return REJECTED;
            case "CANCELED":
            case "CANCELLED": return CANCELED; // accept both spellings
            default:
                // also accept display names case-insensitively
                if ("pending".equalsIgnoreCase(v)) return PENDING;
                if ("approved".equalsIgnoreCase(v)) return APPROVED;
                if ("rejected".equalsIgnoreCase(v)) return REJECTED;
                if ("canceled".equalsIgnoreCase(v) || "cancelled".equalsIgnoreCase(v)) return CANCELED;
                throw new IllegalArgumentException("Unknown RoleRequestStatus: " + s);
        }
    }
}