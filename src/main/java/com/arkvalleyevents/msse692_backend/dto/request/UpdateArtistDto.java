package com.arkvalleyevents.msse692_backend.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateArtistDto {
    private String name;
    private String genre;
    private String biography;
    private String website;
}
