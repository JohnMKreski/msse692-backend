package com.arkvalleyevents.msse692_backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
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
    private Long id;
    private String eventType;
    private String eventName;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalDateTime eventDateTime;
    private String eventLocation;
    private String eventDescription;

    // Nested DTOs instead of entity references
    private VenueDto venue;
    private Set<ArtistDto> artists;

    // Supporting data
    private Set<String> imageUrls;
}
