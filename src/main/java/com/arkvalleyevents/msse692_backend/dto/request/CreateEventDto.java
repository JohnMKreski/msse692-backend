package com.arkvalleyevents.msse692_backend.dto.request;

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
    @NotBlank
    private String eventType;

    @NotBlank
    private String eventName;

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private LocalTime eventTime;

    private LocalDateTime eventDateTime;

    @NotBlank
    private String eventLocation;

    private String eventDescription;

    // References by ID instead of embedding full objects
    private Long venueId;
    private Set<Long> artistIds;

    // Image URLs
    private Set<String> imageUrls;
}
