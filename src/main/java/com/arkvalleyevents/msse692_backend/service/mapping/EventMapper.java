package com.arkvalleyevents.msse692_backend.service.mapping;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.model.Event;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
        // If EventDetailDto contains VenueDto/ArtistDto, add:
        // , uses = { VenueMapper.class, ArtistMapper.class }
)
public interface EventMapper {

    // Create: map DTO -> entity
    // -------- Create --------
    // DB generates ID
    @Mapping(target = "eventId", ignore = true)
    // entity defaults to DRAFT
    @Mapping(target = "status", ignore = true)
    // Set in service impl
    @Mapping(target = "slug", ignore = true)

    Event toEntity(CreateEventDto src);

    // -------- Update (partial merge; ignores nulls) --------
    // Pass the entity to update as the first parameter, the DTO as the second
    // target is an existing Event loaded from DB that is mutating in place.
    void updateEntity(@MappingTarget Event target, UpdateEventDto src);

    // ---------- To summary DTO ----------
    @Mappings({
            @Mapping(target = "typeDisplayName",
                    expression = "java(src.getEventType() != null ? src.getEventType().getTypeDisplayName() : null)"),
            @Mapping(target = "statusDisplayName",
                    expression = "java(src.getStatus() != null ? src.getStatus().getStatusDisplayName() : null)"),
            // if you expose only IDs for related entities in summary:
            @Mapping(target = "venueId",
                    expression = "java(src.getVenue() != null ? src.getVenue().getVenueId() : null)")
    })
    EventDto toDto(Event src);

    // ---------- To detail DTO ----------
    @Mappings({
            @Mapping(target = "typeDisplayName",
                    expression = "java(src.getEventType() != null ? src.getEventType().getTypeDisplayName() : null)"),
            @Mapping(target = "statusDisplayName",
                    expression = "java(src.getStatus() != null ? src.getStatus().getStatusDisplayName() : null)")
            // If VenueDto/ArtistDto are mapped via used mappers, no extra mapping needed here
    })
    EventDetailDto toDetailDto(Event src);

}

/**
 * Notes
 * ReportingPolicy.ERROR helps catch field drift between entity and DTOs.
 * NullValuePropertyMappingStrategy.IGNORE makes updateEntity behave like a PATCH (nulls don’t overwrite).
 * If you generate slug in the service or a listener, keep it ignored here; otherwise add an @AfterMapping to derive it.
 */