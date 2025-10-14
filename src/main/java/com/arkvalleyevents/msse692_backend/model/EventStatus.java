package com.arkvalleyevents.msse692_backend.model;

import lombok.Getter;

@Getter
public enum EventStatus {
    DRAFT("Draft"),
    PUBLISHED("Published"),
    UNPUBLISHED("Unpublished"),
    CANCELLED("Cancelled");;

    private final String statusDisplayName;

    EventStatus(String statusDisplayName) {
        this.statusDisplayName = statusDisplayName;
    }

    //Helpful for logging
    @Override
    public String toString() {
        return statusDisplayName;
    }

    public static EventStatus fromString(String s) {
        if (s == null) return null;
        return switch (s.trim().toUpperCase()) {
            case "DRAFT" -> DRAFT;
            case "PUBLISHED" -> PUBLISHED;
            case "UNPUBLISHED" -> UNPUBLISHED;
            case "CANCELLED" -> CANCELLED;
            default -> throw new IllegalArgumentException("Unknown status: " + s);
        };
    }
}


    // Add other statuses as needed
    // Example: SCHEDULED, COMPLETED, POSTPONED, etc.

    /**
     * Each status can represent a different stage in the event lifecycle
     * You can also add methods or properties if needed for more complex behavior
     * For example, you might want to add a method to check if an event is active
     * or to get a user-friendly name for each status
     * Example:
     * java
     * public String getDisplayName() {
     *     switch (this) {
     *         case DRAFT: return "Draft";
     *         case PUBLISHED: return "Published";
     *         case CANCELLED: return "Cancelled";
     *         default: return "Unknown";
     *     }
     * }
     */

    /**
     * You can also implement interfaces if you want to add more functionality
     * Example: Comparable<EventStatus> if you want to define an order for statuses
     */

    /**
     * Remember to keep the enum simple and focused on representing the state of an event
     * Enums are a great way to represent a fixed set of constants in a type-safe manner
     * They improve code readability and maintainability
     * and help prevent invalid values from being used
     * in your application
     * Always consider the specific needs of your application when designing enums
     * and avoid overcomplicating them with too many responsibilities
     * Keep it focused on the core concept it represents
     * This enum can be expanded or modified as your application evolves
     * and new requirements emerge
     *
     */
