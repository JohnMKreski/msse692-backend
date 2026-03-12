package com.arkvalleyevents.msse692_backend.service.mapping;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.model.Event;
import com.arkvalleyevents.msse692_backend.model.EventType;
import com.arkvalleyevents.msse692_backend.util.CommunityTimezone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperConversionTest {

    private final EventMapper mapper = Mappers.getMapper(EventMapper.class);

    @BeforeEach
    void setUp() {
        // No-op. Conversions are pinned to America/Denver via CommunityTimezone.
    }

    @AfterEach
    void tearDown() {
        // No-op.
    }

    @Test
    void toEntity_mapsDenverLocalDateTimeToInstant() {
        CreateEventDto dto = new CreateEventDto();
        dto.setEventName("Time Test");
        dto.setType(EventType.CONCERT);
        // Nov 19, 2025 is MST (UTC-07). 19:00 MST -> 02:00Z.
        dto.setStartAt(LocalDateTime.of(2025, 11, 19, 19, 0));
        dto.setEndAt(LocalDateTime.of(2025, 11, 19, 21, 0));

        Event entity = mapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(Instant.parse("2025-11-20T02:00:00Z"), entity.getStartAt());
        assertEquals(Instant.parse("2025-11-20T04:00:00Z"), entity.getEndAt());
    }

    @Test
    void updateEntity_mapsDenverLocalDateTimePatch_onExistingEntity() {
        Event existing = new Event();
        existing.setEventName("Old");
        existing.setStartAt(Instant.parse("2025-01-01T17:00:00Z"));
        existing.setEndAt(Instant.parse("2025-01-01T19:00:00Z"));

        UpdateEventDto patch = new UpdateEventDto();
        patch.setEventName("New");
        LocalDateTime startLocal = LocalDateTime.of(2025, 3, 21, 19, 0);
        LocalDateTime endLocal = LocalDateTime.of(2025, 3, 21, 21, 30);
        patch.setStartAt(startLocal);
        patch.setEndAt(endLocal);

        mapper.updateEntity(existing, patch);

        assertEquals("New", existing.getEventName());
        assertEquals(CommunityTimezone.toInstant(startLocal), existing.getStartAt());
        assertEquals(CommunityTimezone.toInstant(endLocal), existing.getEndAt());
    }

    @Test
    void roundTrip_normalDate_preservesDenverWallTime() {
        // A normal (non-transition) date/time should round-trip:
        // Denver wall time -> stored Instant -> Denver OffsetDateTime has same wall time.
        LocalDateTime startLocal = LocalDateTime.of(2026, 2, 1, 19, 15);
        LocalDateTime endLocal = LocalDateTime.of(2026, 2, 1, 20, 45);

        CreateEventDto create = new CreateEventDto();
        create.setEventName("Round Trip Test");
        create.setType(EventType.CONCERT);
        create.setStartAt(startLocal);
        create.setEndAt(endLocal);

        Event entity = mapper.toEntity(create);
        assertNotNull(entity);
        assertEquals(CommunityTimezone.toInstant(startLocal), entity.getStartAt());
        assertEquals(CommunityTimezone.toInstant(endLocal), entity.getEndAt());

        EventDto dto = mapper.toDto(entity);
        assertNotNull(dto);
        assertNotNull(dto.getStartAt());
        assertNotNull(dto.getEndAt());
        assertEquals(startLocal, dto.getStartAt().toLocalDateTime());
        assertEquals(endLocal, dto.getEndAt().toLocalDateTime());
    }
}
