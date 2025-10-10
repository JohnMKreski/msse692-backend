package com.arkvalleyevents.msse692_backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

/**
 * Use this for GET /venues/{id} (venue profile page).
 * Includes summary EventDto list for events at that venue.
 */

@Data
@NoArgsConstructor
public class VenueDetailDto {
    private Long venueId;
    private String name;
    private String address;
    private Integer capacity;
    private String description;
    private String website;

    // List of events hosted at this venue
    private Set<EventDto> events;
}
