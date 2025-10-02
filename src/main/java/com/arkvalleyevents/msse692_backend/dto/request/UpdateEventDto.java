package com.arkvalleyevents.msse692_backend.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
public class UpdateEventDto {
    private String eventType;
    private String eventName;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalDateTime eventDateTime;
    private String eventLocation;
    private String eventDescription;

    private Long venueId;
    private Set<Long> artistIds;
    private Set<String> imageUrls;
}
