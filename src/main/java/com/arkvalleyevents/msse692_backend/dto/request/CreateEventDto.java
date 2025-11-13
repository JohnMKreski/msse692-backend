package com.arkvalleyevents.msse692_backend.dto.request;

import com.arkvalleyevents.msse692_backend.model.EventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
public class CreateEventDto {

    private String eventName;
    private EventType type;
    // Accept absolute instants from clients (e.g., "2025-11-20T02:00:00Z").
    // The service maps these to LocalDateTime in the server's zone (e.g., America/Denver) for storage/display.
    private Instant startAt;
    private Instant endAt;
    private String eventLocation;
    private String eventDescription;

    // References by ID instead of embedding full objects
//    private Long venueId;
//    private Set<Long> artistIds;
//
//    // Image URLs
//    private Set<String> imageUrls;
}
