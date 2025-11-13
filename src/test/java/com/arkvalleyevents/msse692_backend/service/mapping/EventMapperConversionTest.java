package com.arkvalleyevents.msse692_backend.service.mapping;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.model.Event;
import com.arkvalleyevents.msse692_backend.model.EventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperConversionTest {

    private final EventMapper mapper = Mappers.getMapper(EventMapper.class);
    private TimeZone originalTz;

    @BeforeEach
    void setUp() {
        originalTz = TimeZone.getDefault();
        // Ensure conversion uses a predictable server zone for the test
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTz);
    }

    @Test
    void toEntity_mapsInstantToLocalDateTime_inServerZone() {
        CreateEventDto dto = new CreateEventDto();
        dto.setEventName("Time Test");
        dto.setType(EventType.CONCERT);
        dto.setStartAt(Instant.parse("2025-11-20T02:00:00Z")); // 2025-11-19T19:00 in America/Denver
        dto.setEndAt(Instant.parse("2025-11-20T04:00:00Z"));   // 2025-11-19T21:00 in America/Denver

        Event entity = mapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(LocalDateTime.of(2025, 11, 19, 19, 0), entity.getStartAt());
        assertEquals(LocalDateTime.of(2025, 11, 19, 21, 0), entity.getEndAt());
    }

    @Test
    void updateEntity_mapsInstantPatch_onExistingEntity() {
        Event existing = new Event();
        existing.setEventName("Old");
        existing.setStartAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        existing.setEndAt(LocalDateTime.of(2025, 1, 1, 12, 0));

        UpdateEventDto patch = new UpdateEventDto();
        patch.setEventName("New");
        patch.setStartAt(Instant.parse("2025-03-22T01:00:00Z")); // 2025-03-21T19:00 MDT
        patch.setEndAt(Instant.parse("2025-03-22T03:30:00Z"));   // 2025-03-21T21:30 MDT

        mapper.updateEntity(existing, patch);

        assertEquals("New", existing.getEventName());
        assertEquals(LocalDateTime.of(2025, 3, 21, 19, 0), existing.getStartAt());
        assertEquals(LocalDateTime.of(2025, 3, 21, 21, 30), existing.getEndAt());
    }
}
