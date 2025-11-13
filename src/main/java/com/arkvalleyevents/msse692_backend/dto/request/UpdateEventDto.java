package com.arkvalleyevents.msse692_backend.dto.request;

import com.arkvalleyevents.msse692_backend.model.EventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
public class UpdateEventDto {

    private String eventName;
    private EventType type;

    // Accept absolute instants from clients; mapped to LocalDateTime in server's zone
    private Instant startAt;
    private Instant endAt;

    private String eventLocation;
    private String eventDescription;

//    private Long venueId;
//    private Set<Long> artistIds;
//    private Set<String> imageUrls;
}
