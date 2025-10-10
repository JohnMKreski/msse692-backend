package com.arkvalleyevents.msse692_backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

/**
 * Use this for GET /artists/{id} (artist profile page).
 * Only includes lightweight EventDto references (id, name, date), not full nested EventDetailDto — avoids deep recursion.
 */

@Data
@NoArgsConstructor
public class ArtistDetailDto {
    private Long artistId;
    private String name;
    private String genre;
    private String biography;
    private String website;

    // Optional: list of events this artist is linked to
    private Set<EventDto> events;
}
