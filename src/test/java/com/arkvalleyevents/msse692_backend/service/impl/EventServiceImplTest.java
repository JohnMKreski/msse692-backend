package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.model.Event;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import com.arkvalleyevents.msse692_backend.model.EventType;
import com.arkvalleyevents.msse692_backend.repository.EventRepository;
import com.arkvalleyevents.msse692_backend.service.EventAuditService;
import com.arkvalleyevents.msse692_backend.service.mapping.EventMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventMapper mapper;
    @Mock private EventAuditService auditService;

    @InjectMocks private EventServiceImpl service;

    @Captor private ArgumentCaptor<Event> eventCaptor;
    @Captor private ArgumentCaptor<Pageable> pageableCaptor;

    private CreateEventDto newEventDto;
    private Event mappedNewEntity;

    @BeforeEach
    void setUp() {
        newEventDto = new CreateEventDto();
        newEventDto.setEventName("Spring Bash 2025");
        newEventDto.setType(EventType.CONCERT);
        newEventDto.setStartAt(LocalDateTime.of(2025, 3, 21, 18, 0));
        newEventDto.setEndAt(LocalDateTime.of(2025, 3, 21, 21, 0));
        newEventDto.setEventLocation("Salida, CO");

        mappedNewEntity = new Event();
        mappedNewEntity.setEventName(newEventDto.getEventName());
        mappedNewEntity.setEventLocation(newEventDto.getEventLocation());
        mappedNewEntity.setStartAt(newEventDto.getStartAt());
        mappedNewEntity.setEndAt(newEventDto.getEndAt());
        mappedNewEntity.setEventType(newEventDto.getType());
        mappedNewEntity.setStatus(EventStatus.DRAFT);
    }

    @Test
    void createEvent_generatesUniqueSlug_andLogsCreate() {
        when(mapper.toEntity(newEventDto)).thenReturn(mappedNewEntity);
        // First slug taken, first increment taken, second increment available -> expect -2
        when(eventRepository.findBySlug("spring-bash-2025")).thenReturn(Optional.of(new Event()));
        when(eventRepository.findBySlug("spring-bash-2025-1")).thenReturn(Optional.of(new Event()));
        when(eventRepository.findBySlug("spring-bash-2025-2")).thenReturn(Optional.empty());

        Event saved = new Event();
        saved.setEventId(42L);
        saved.setStatus(EventStatus.DRAFT);
        saved.setSlug("spring-bash-2025-2");
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventDetailDto detail = new EventDetailDto();
        detail.setEventId(42L);
        detail.setSlug("spring-bash-2025-2");
        detail.setStatus(EventStatus.DRAFT);
        when(mapper.toDetailDto(saved)).thenReturn(detail);

        EventDetailDto result = service.createEvent(newEventDto);

        assertNotNull(result);
        assertEquals(42L, result.getEventId());
        assertEquals("spring-bash-2025-2", result.getSlug());

        verify(eventRepository).save(eventCaptor.capture());
        assertEquals("spring-bash-2025-2", eventCaptor.getValue().getSlug());

        verify(auditService).logCreate(42L);
    }

    @Test
    void publishEvent_fromDraft_setsPublished_andAudits() {
        Event existing = new Event();
        existing.setEventId(1L);
        existing.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));

        Event afterSave = new Event();
        afterSave.setEventId(1L);
        afterSave.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.save(any(Event.class))).thenReturn(afterSave);

        EventDetailDto dto = new EventDetailDto();
        dto.setEventId(1L);
        dto.setStatus(EventStatus.PUBLISHED);
        when(mapper.toDetailDto(afterSave)).thenReturn(dto);

        EventDetailDto result = service.publishEvent(1L);

        assertEquals(EventStatus.PUBLISHED, result.getStatus());
        verify(eventRepository).save(eventCaptor.capture());
        assertEquals(EventStatus.PUBLISHED, eventCaptor.getValue().getStatus());
        verify(auditService).logUpdate(1L);
    }

    @Test
    void publishEvent_whenNotDraft_throwsIllegalState() {
        Event existing = new Event();
        existing.setEventId(5L);
        existing.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.publishEvent(5L));
        verify(eventRepository, never()).save(any());
        verify(auditService, never()).logUpdate(anyLong());
    }

    @Test
    void unpublishEvent_fromPublished_setsUnpublished() {
        Event existing = new Event();
        existing.setEventId(2L);
        existing.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(existing));

        Event afterSave = new Event();
        afterSave.setEventId(2L);
        afterSave.setStatus(EventStatus.UNPUBLISHED);
        when(eventRepository.save(any(Event.class))).thenReturn(afterSave);

        EventDetailDto dto = new EventDetailDto();
        dto.setEventId(2L);
        dto.setStatus(EventStatus.UNPUBLISHED);
        when(mapper.toDetailDto(afterSave)).thenReturn(dto);

        EventDetailDto result = service.unpublishEvent(2L);
        assertEquals(EventStatus.UNPUBLISHED, result.getStatus());
        verify(auditService).logUpdate(2L);
    }

    @Test
    void unpublishEvent_whenNotPublished_throwsIllegalState() {
        Event existing = new Event();
        existing.setEventId(7L);
        existing.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(7L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.unpublishEvent(7L));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void cancelEvent_changesStatusUnlessAlreadyCancelled() {
        Event existing = new Event();
        existing.setEventId(3L);
        existing.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(3L)).thenReturn(Optional.of(existing));

        Event afterSave = new Event();
        afterSave.setEventId(3L);
        afterSave.setStatus(EventStatus.CANCELLED);
        when(eventRepository.save(any(Event.class))).thenReturn(afterSave);

        EventDetailDto dto = new EventDetailDto();
        dto.setEventId(3L);
        dto.setStatus(EventStatus.CANCELLED);
        when(mapper.toDetailDto(afterSave)).thenReturn(dto);

        EventDetailDto result = service.cancelEvent(3L);
        assertEquals(EventStatus.CANCELLED, result.getStatus());
        verify(auditService).logUpdate(3L);
    }

    @Test
    void cancelEvent_whenAlreadyCancelled_throws() {
        Event existing = new Event();
        existing.setEventId(8L);
        existing.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(8L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.cancelEvent(8L));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEvent_mergesNonNulls_saves_andAudits() {
        Event existing = new Event();
        existing.setEventId(11L);
        existing.setEventName("Old name");
        when(eventRepository.findById(11L)).thenReturn(Optional.of(existing));

        UpdateEventDto patch = new UpdateEventDto();
        patch.setEventName("New name");

        doAnswer(inv -> {
            Event target = inv.getArgument(0);
            UpdateEventDto src = inv.getArgument(1);
            if (src.getEventName() != null) target.setEventName(src.getEventName());
            return null;
        }).when(mapper).updateEntity(any(Event.class), any(UpdateEventDto.class));

        Event saved = new Event();
        saved.setEventId(11L);
        saved.setEventName("New name");
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventDetailDto dto = new EventDetailDto();
        dto.setEventId(11L);
        dto.setEventName("New name");
        when(mapper.toDetailDto(saved)).thenReturn(dto);

        EventDetailDto result = service.updateEvent(11L, patch);

        assertEquals("New name", result.getEventName());
        verify(auditService).logUpdate(11L);
    }

    @Test
    void deleteEvent_whenExists_deletesAndAudits() {
        when(eventRepository.existsById(99L)).thenReturn(true);

        service.deleteEvent(99L);

        verify(auditService).logDelete(99L);
        verify(eventRepository).deleteById(99L);
    }

    @Test
    void deleteEvent_whenMissing_throwsNotFound() {
        when(eventRepository.existsById(100L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> service.deleteEvent(100L));
        verify(auditService, never()).logDelete(anyLong());
        verify(eventRepository, never()).deleteById(anyLong());
    }

    @Test
    void getEventById_whenFound_returnsDetailDto() {
        Event entity = new Event();
        entity.setEventId(5L);
        when(eventRepository.findById(5L)).thenReturn(Optional.of(entity));

        EventDetailDto dto = new EventDetailDto();
        dto.setEventId(5L);
        when(mapper.toDetailDto(entity)).thenReturn(dto);

        Optional<EventDetailDto> result = service.getEventById(5L);
        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getEventId());
    }

    @Test
    void getEventById_whenMissing_throwsNotFound() {
        when(eventRepository.findById(123L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.getEventById(123L));
    }

    @Test
    void getEventBySlug_whenFound_returnsDetailDto() {
        Event entity = new Event();
        entity.setEventId(6L);
        entity.setSlug("summer-fest");
        when(eventRepository.findBySlug("summer-fest")).thenReturn(Optional.of(entity));

        EventDetailDto dto = new EventDetailDto();
        dto.setEventId(6L);
        dto.setSlug("summer-fest");
        when(mapper.toDetailDto(entity)).thenReturn(dto);

        EventDetailDto result = service.getEventBySlug("summer-fest");
        assertEquals(6L, result.getEventId());
        assertEquals("summer-fest", result.getSlug());
    }

    @Test
    void listEvents_usesParsedSort_andMapsDtos() {
        Event e1 = new Event(); e1.setEventId(1L);
        Event e2 = new Event(); e2.setEventId(2L);
        Page<Event> page = new PageImpl<>(List.of(e1, e2));
        when(eventRepository.findAll(any(Pageable.class))).thenReturn(page);

        EventDto d1 = new EventDto(); d1.setEventId(1L);
        EventDto d2 = new EventDto(); d2.setEventId(2L);
        when(mapper.toDto(e1)).thenReturn(d1);
        when(mapper.toDto(e2)).thenReturn(d2);

        List<EventDto> result = service.listEvents(Map.of(), 0, 10, "eventName,desc");
        assertEquals(2, result.size());

        verify(eventRepository).findAll(pageableCaptor.capture());
        Pageable p = pageableCaptor.getValue();
        assertEquals(0, p.getPageNumber());
        assertEquals(10, p.getPageSize());
        assertTrue(p.getSort().getOrderFor("eventName").isDescending());
    }

    @Test
    void listUpcoming_usesLimitAndSortAscending() {
        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        Event e = new Event(); e.setEventId(9L);
        Page<Event> page = new PageImpl<>(List.of(e));
        when(eventRepository.findByStartAtAfter(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        EventDto dto = new EventDto(); dto.setEventId(9L);
        when(mapper.toDto(e)).thenReturn(dto);

        List<EventDto> result = service.listUpcoming(from, 5);
        assertEquals(1, result.size());

        verify(eventRepository).findByStartAtAfter(eq(from), pageableCaptor.capture());
        Pageable p = pageableCaptor.getValue();
        assertEquals(0, p.getPageNumber());
        assertEquals(5, p.getPageSize());
        Sort.Order order = p.getSort().getOrderFor("startAt");
        assertNotNull(order);
        assertTrue(order.isAscending());
    }

    @Test
    void getEventsByType_mapsList() {
        Event e1 = new Event(); e1.setEventId(1L);
        when(eventRepository.findByEventType(EventType.CONCERT)).thenReturn(List.of(e1));
        EventDto d1 = new EventDto(); d1.setEventId(1L);
        when(mapper.toDto(e1)).thenReturn(d1);

        List<EventDto> result = service.getEventsByType(EventType.CONCERT);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getEventId());
    }

    @Test
    void getEventsByDate_callsRepositoryWithDayBounds() {
        LocalDate date = LocalDate.of(2025, 4, 15);
        Event e1 = new Event(); e1.setEventId(1L);

        when(eventRepository.findByStartAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(e1));

        EventDto d1 = new EventDto(); d1.setEventId(1L);
        when(mapper.toDto(e1)).thenReturn(d1);

        List<EventDto> result = service.getEventsByDate(date);
        assertEquals(1, result.size());

        ArgumentCaptor<LocalDateTime> startCap = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCap = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(eventRepository).findByStartAtBetween(startCap.capture(), endCap.capture());

        LocalDateTime expectedStart = date.atStartOfDay();
        LocalDateTime expectedEnd = date.plusDays(1).atStartOfDay().minusNanos(1);
        assertEquals(expectedStart, startCap.getValue());
        assertEquals(expectedEnd, endCap.getValue());
    }

    @Test
    void getEventsByLocation_filtersAndMaps() {
        Event e1 = new Event(); e1.setEventId(1L);
        when(eventRepository.findByEventLocationContainingIgnoreCase("salida"))
                .thenReturn(List.of(e1));
        EventDto d1 = new EventDto(); d1.setEventId(1L);
        when(mapper.toDto(e1)).thenReturn(d1);

        List<EventDto> result = service.getEventsByLocation("salida");
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getEventId());
    }
}