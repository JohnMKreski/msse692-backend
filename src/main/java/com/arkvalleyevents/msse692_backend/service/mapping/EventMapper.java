package com.arkvalleyevents.msse692_backend.service.mapping;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.model.Event;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EventMapper {

    // Create: map DTO -> entity (ignore id so JPA can generate it)
    @Mapping(target = "id", ignore = true)
    Event toEntity(CreateEventDto dto);

    // Read: map entity -> response DTOs
    EventDetailDto toDetailDto(Event entity);

    EventDto toDto(Event entity);

    // Update: apply non-null fields from DTO onto the existing entity
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateEventDto dto, @MappingTarget Event entity);
}