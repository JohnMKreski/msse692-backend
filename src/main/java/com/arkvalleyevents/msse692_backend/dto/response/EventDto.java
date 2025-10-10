package com.arkvalleyevents.msse692_backend.dto.response;

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
    private String eventType;
    private String eventName;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalDateTime eventDateTime; //Combine date and time?
    private String eventLocation;
    private String eventDescription;

    // Flattened relationships
    private String venueName;
    private Set<String> artistNames;

    // Supporting data
    private Set<String> imageUrls;
}
