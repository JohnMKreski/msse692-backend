package com.arkvalleyevents.msse692_backend.dto.request;

import com.arkvalleyevents.msse692_backend.model.EventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
public class CreateEventDto {

    private String eventName;
    private EventType type;
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
