package com.arkvalleyevents.msse692_backend.dto.request;

import com.arkvalleyevents.msse692_backend.model.EventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CreateEventDto {

    private String eventName;
    private EventType type;
    
    // Changed from Instant to LocalDateTime to accept "wall clock" times in the community timezone.
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private String eventLocation;
    private String eventDescription;

    // References by ID instead of embedding full objects
//    private Long venueId;
//    private Set<Long> artistIds;
//
//    // Image URLs
//    private Set<String> imageUrls;
}
