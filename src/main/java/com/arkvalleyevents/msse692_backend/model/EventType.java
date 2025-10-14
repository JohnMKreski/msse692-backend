package com.arkvalleyevents.msse692_backend.model;

import lombok.Getter;

@Getter
public enum EventType {
    CONCERT("Concert"),
    FESTIVAL("Festival"),
    PARTY("Party"),
    OTHER("Other");

    private final String typeDisplayName;

    EventType(String typeDisplayName) {
        this.typeDisplayName = typeDisplayName;
    }

    //Helpful for logging
    @Override
    public String toString() {
        return typeDisplayName;
    }

    public static com.arkvalleyevents.msse692_backend.model.EventType fromString(String s) {
        if (s == null) return null;
        return switch (s.trim().toUpperCase()) {
            case "CONCERT" -> CONCERT;
            case "FESTIVAL" -> FESTIVAL;
            case "PARTY" -> PARTY;
            case "OTHER" -> OTHER;
            default -> throw new IllegalArgumentException("Unknown type: " + s);
        };
    }
}
