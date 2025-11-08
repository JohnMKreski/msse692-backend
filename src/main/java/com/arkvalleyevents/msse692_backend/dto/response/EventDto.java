package com.arkvalleyevents.msse692_backend.dto.response;

import com.arkvalleyevents.msse692_backend.model.EventStatus;
import com.arkvalleyevents.msse692_backend.model.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
public class EventDto {

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
//    private String eventDescription; TODO: Consider adding a short description field for listing views

    // Ownership (populated by JPA auditing)
    private Long createdByUserId;
    private Long lastModifiedByUserId;

    // Flattened relationships
//    private String venueName;
//    private Set<String> artistNames;

    // Supporting data
//    private Set<String> imageUrls; TODO: or Hero Image URL (first of imageUrls)
}
