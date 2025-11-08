package com.arkvalleyevents.msse692_backend.dto.response;

import com.arkvalleyevents.msse692_backend.model.EventStatus;
import com.arkvalleyevents.msse692_backend.model.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * Composes VenueDto and ArtistDto instead of duplicating their fields.
 * Use this for GET /events/{id} (detailed event page).
 */

@Data
@NoArgsConstructor
public class EventDetailDto {
    private Long eventId;
    private String slug;
    private String eventName;

    private EventType type;
    private String typeDisplayName; // Computed field for UI display

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private EventStatus status;
    private String statusDisplayName; // Computed field for UI display

    private String eventLocation;
    private String eventDescription;

    // Nested DTOs instead of entity references
//    private VenueDto venue;
//    private Set<ArtistDto> artists;
//
//    // Supporting data
//    private Set<String> imageUrls;

    private Instant createdAt;
    private Instant updatedAt;

    // Ownership (populated by JPA auditing)
    private Long createdByUserId;
    private Long lastModifiedByUserId;
}
