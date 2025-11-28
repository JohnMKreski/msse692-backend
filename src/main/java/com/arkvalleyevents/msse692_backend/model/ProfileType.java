package com.arkvalleyevents.msse692_backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProfileType {
    VENUE("Venue"),
    ARTIST("Artist"),
    OTHER("Other");

    private final String typeProfileType;

    ProfileType(String typeProfileType) {
        this.typeProfileType = typeProfileType;
    }

    //Helpful for logging
    @Override
    public String toString() {
        return typeProfileType;
    }

    @JsonValue
    public String getDisplayName() {
        return typeProfileType;
    }

    @JsonCreator
    public static com.arkvalleyevents.msse692_backend.model.ProfileType fromString(String s) {
        if (s == null) return null;
        return switch (s.trim().toUpperCase()) {
            case "VENUE" -> VENUE;
            case "ARTIST" -> ARTIST;
            case "OTHER" -> OTHER;
            default -> throw new IllegalArgumentException("Unknown type: " + s);
        };
    }
}

