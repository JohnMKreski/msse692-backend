package com.arkvalleyevents.msse692_backend.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class CreateVenueDto {
    @NotBlank
    private String name;

    @NotBlank
    private String address;

    private Integer capacity;
    private String description;
    private String website;
}
