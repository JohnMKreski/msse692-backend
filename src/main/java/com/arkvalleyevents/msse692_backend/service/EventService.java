package com.arkvalleyevents.msse692_backend.service;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface EventService {

    // =========================
    // Commands (state changes)
    // =========================

    /** Create a new event and return its detailed view. */
    EventDetailDto createEvent(CreateEventDto input);

    /** Update an existing event and return its detailed view. */
    EventDetailDto updateEvent(Long eventId, UpdateEventDto input);

    /** Publish/unpublish/cancel return the resulting detailed view. */
    EventDetailDto publishEvent(Long eventId);
    EventDetailDto unpublishEvent(Long eventId);
    EventDetailDto cancelEvent(Long eventId);

    /** Hard delete (or implement soft delete behind the scenes). */
    void deleteEvent(Long eventId);


    // =========================
    // Queries (no state change)
    // =========================

    /** Fetch one by id or slug with full detail. */
    EventDetailDto getEventById(Long eventId);
    EventDetailDto getEventBySlug(String slug);

    /**
     * General list/search endpoint:
     * - filters: free-form map (e.g., type, dateFrom, dateTo, location, status)
     * - page/size/sort: simple paging if you’re not using Pageable
     */
    List<EventDto> listEvents(Map<String, String> filters, int page, int size, String sort);

    /** Lightweight helper for “what’s coming up from time X” with a hard cap. */
    List<EventDto> listUpcoming(LocalDateTime from, int limit);

    /** Optional convenience queries (can be folded into listEvents via filters). */
    List<EventDto> getAllEvents();
    List<EventDto> getEventsByType(String eventType);
    List<EventDto> getEventsByDate(String eventDate);
    List<EventDto> getEventsByLocation(String eventLocation);
}
