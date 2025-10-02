package com.arkvalleyevents.msse692_backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ArtistDto {
    private Long id;
    private String name;
    private String genre;
    private String biography;
    private String website;
}
