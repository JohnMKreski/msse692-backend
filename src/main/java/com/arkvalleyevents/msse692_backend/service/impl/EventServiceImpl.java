package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.service.EventService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Import your model and repository
// Adjust package names to match your project.
import com.arkvalleyevents.msse692_backend.model.Event;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import com.arkvalleyevents.msse692_backend.repository.EventRepository;

// A mapper component you provide (MapStruct/ModelMapper/manual).
// Define with methods: toEntity(CreateEventDto), toDetailDto(Event), toDto(Event), updateEntity(UpdateEventDto, Event).
import com.arkvalleyevents.msse692_backend.service.mapping.EventMapper;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper mapper;

    public EventServiceImpl(EventRepository eventRepository, EventMapper mapper) {
        this.eventRepository = eventRepository;
        this.mapper = mapper;
    }

    // Create
    @Override
    public EventDetailDto createEvent(CreateEventDto request) {
        Event entity = mapper.toEntity(request);
        if (entity.getStatus() == null) {
            entity.setStatus(EventStatus.DRAFT);
        }
        Event saved = eventRepository.save(entity);
        return mapper.toDetailDto(saved);
    }

    // Read
    @Override
    @Transactional(readOnly = true)
    public EventDetailDto getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        return mapper.toDetailDto(event);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDetailDto getEventBySlug(String slug) {
        Event event = eventRepository.findBySlug(slug) // TODO: implement in EventRepository
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + slug));
        return mapper.toDetailDto(event);
    }

    // List/Search
    @Override
    @Transactional(readOnly = true)
    public List<EventDto> listEvents(Map<String, String> filters, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), parseSort(sort));
        // TODO: apply filters with Specification if needed; using simple paging for now
        Page<Event> pageResult = eventRepository.findAll(pageable);
        return pageResult.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> listUpcoming(LocalDateTime from, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(limit, 1), Sort.by(Sort.Direction.ASC, "startAt")); // adjust field name
        List<Event> events = eventRepository.findByStartAtAfter(from, pageable); // TODO: implement in EventRepository
        return events.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByType(String eventType) {
        List<Event> events = eventRepository.findByTypeIgnoreCase(eventType); // TODO: implement in EventRepository
        return events.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByDate(String eventDate) {
        LocalDate date = LocalDate.parse(eventDate); // expects ISO‑8601 yyyy‑MM‑dd
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
        List<Event> events = eventRepository.findByStartAtBetween(start, end); // TODO: implement in EventRepository
        return events.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByLocation(String eventLocation) {
        List<Event> events = eventRepository.findByLocationContainingIgnoreCase(eventLocation); // TODO: implement in EventRepository
        return events.stream().map(mapper::toDto).toList();
    }

    // Update
    @Override
    public EventDetailDto updateEvent(Long eventId, UpdateEventDto request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        mapper.updateEntity(request, event); // partial update (non‑nulls)
        Event saved = eventRepository.save(event);
        return mapper.toDetailDto(saved);
    }

    // Status changes
    @Override
    public EventDetailDto publishEvent(Long eventId) {
        Event event = load(eventId);
        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);
        return mapper.toDetailDto(saved);
    }

    @Override
    public EventDetailDto unpublishEvent(Long eventId) {
        Event event = load(eventId);
        event.setStatus(EventStatus.DRAFT);
        Event saved = eventRepository.save(event);
        return mapper.toDetailDto(saved);
    }

    @Override
    public EventDetailDto cancelEvent(Long eventId) {
        Event event = load(eventId);
        event.setStatus(EventStatus.CANCELLED);
        Event saved = eventRepository.save(event);
        return mapper.toDetailDto(saved);
    }

    // Delete
    @Override
    public void deleteEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found: " + eventId);
        }
        eventRepository.deleteById(eventId);
    }

    // Helpers
    private Event load(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }
        String s = sort.trim();
        if (s.startsWith("-")) {
            return Sort.by(Sort.Direction.DESC, s.substring(1));
        }
        if (s.endsWith(",desc")) {
            return Sort.by(Sort.Direction.DESC, s.substring(0, s.length() - 5));
        }
        if (s.endsWith(",asc")) {
            return Sort.by(Sort.Direction.ASC, s.substring(0, s.length() - 4));
        }
        return Sort.by(Sort.Direction.ASC, s);
    }
}