package com.arkvalleyevents.msse692_backend.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class CreateArtistDto {
    @NotBlank
    private String name;

    @NotBlank
    private String genre;

    private String biography;
    private String website;
}
