package com.arkvalleyevents.msse692_backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VenueDto {
    private Long id;
    private String name;
    private String address;
    private Integer capacity;
    private String description;
    private String website;
}
