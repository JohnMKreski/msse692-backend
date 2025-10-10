package com.arkvalleyevents.msse692_backend.service;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


public interface EventService {
    //Create
    CreateEventDto createEvent(CreateEventDto EventDetailDto);

    //Read
    EventDetailDto getEventById(Long eventId);
    EventDetailDto getEventBySlug(String slug);

    //List/Search
    List<EventDto> listEvents(Map<String, String> filters, int page, int size, String sort);
    List<EventDto> listUpcoming(LocalDateTime from, int limit);

    List<EventDto> getAllEvents();
    List<EventDto> getEventsByType(String eventType);
    List<EventDto> getEventsByDate(String eventDate);
    List<EventDto> getEventsByLocation(String eventLocation);

    //update
    UpdateEventDto updateEvent(Long eventId, UpdateEventDto EventDetailDto);

    // Status Changes
    EventDetailDto publishEvent(Long eventId);
    EventDetailDto unpublishEvent(Long eventId);
    EventDetailDto cancelEvent(Long eventId);

    // Delete
    void deleteEvent(Long eventId);
}
