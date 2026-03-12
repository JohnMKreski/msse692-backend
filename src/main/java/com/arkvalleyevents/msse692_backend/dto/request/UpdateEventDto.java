package com.arkvalleyevents.msse692_backend.dto.request;

import com.arkvalleyevents.msse692_backend.model.EventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UpdateEventDto {

    private String eventName;
    private EventType type;

    // Denver wall-clock time; backend interprets as America/Denver and converts to Instant.
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private String eventLocation;
    private String eventDescription;

//    private Long venueId;
//    private Set<Long> artistIds;
//    private Set<String> imageUrls;
}
