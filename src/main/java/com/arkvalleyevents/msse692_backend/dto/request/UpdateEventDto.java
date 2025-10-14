package com.arkvalleyevents.msse692_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
public class UpdateEventDto {

    private String eventName;
    private String typeDisplayName;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private String eventLocation;
    private String eventDescription;

//    private Long venueId;
//    private Set<Long> artistIds;
//    private Set<String> imageUrls;
}
