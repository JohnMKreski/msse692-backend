package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.model.Event;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import com.arkvalleyevents.msse692_backend.repository.EventRepository;
import com.arkvalleyevents.msse692_backend.security.policy.EventListPolicy;
import com.arkvalleyevents.msse692_backend.service.EventAuditService;
import com.arkvalleyevents.msse692_backend.service.mapping.EventMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServicePublicUpcomingTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventMapper mapper;
    @Mock private EventAuditService auditService;
    @Mock private EventListPolicy eventListPolicy;

    @InjectMocks private EventServiceImpl service;

    @Test
    void listPublicUpcoming_filtersPublished_andSortsAscending() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        Event e1 = new Event(); e1.setEventId(1L); e1.setStatus(EventStatus.PUBLISHED); e1.setStartAt(now.plusSeconds(2 * 3600));
        Event e2 = new Event(); e2.setEventId(2L); e2.setStatus(EventStatus.PUBLISHED); e2.setStartAt(now.plusSeconds(3 * 3600));
        Page<Event> page = new PageImpl<>(List.of(e1, e2));
        when(eventRepository.findByStatusAndStartAtGreaterThanEqualOrderByStartAtAsc(eq(EventStatus.PUBLISHED), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);
        EventDto d1 = new EventDto(); d1.setEventId(1L); d1.setStatus(EventStatus.PUBLISHED);
        EventDto d2 = new EventDto(); d2.setEventId(2L); d2.setStatus(EventStatus.PUBLISHED);
        when(mapper.toDto(e1)).thenReturn(d1);
        when(mapper.toDto(e2)).thenReturn(d2);

        List<EventDto> result = service.listPublicUpcoming(now, 5);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getEventId());
        assertEquals(2L, result.get(1).getEventId());
        assertTrue(result.stream().allMatch(r -> r.getStatus() == EventStatus.PUBLISHED));
    }

    @Test
    void listPublicUpcoming_emptyWhenNone() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        Page<Event> page = new PageImpl<>(List.of());
        when(eventRepository.findByStatusAndStartAtGreaterThanEqualOrderByStartAtAsc(eq(EventStatus.PUBLISHED), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);
        List<EventDto> result = service.listPublicUpcoming(now, 3);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
