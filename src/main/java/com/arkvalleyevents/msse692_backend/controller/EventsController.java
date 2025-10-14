package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
//Triggers validation of parameters that have constraint annotations (e.g., @NotNull, @Size) and supports validation groups.
@Validated
public class EventsController {
    private static final Logger log = LoggerFactory.getLogger(EventsController.class);

    //Service
    private final EventService eventService;

    public EventsController(EventService eventService) {
        this.eventService = eventService;
        log.info("EventsController initialized");
    }

    private HttpServletRequest request;

    //Post /api/events
    @PostMapping
    public ResponseEntity<EventDetailDto> createEvent(@RequestBody @Valid CreateEventDto dto) {
        log.info("POST /api/events called");
        EventDetailDto created = eventService.createEvent(dto);

        // Build Location: /api/events/{id}]
        /*
        * ServletUriComponentsBuilder - helps to build URIs based on the current request.
        * What it does:
        *   It helps you dynamically build URLs based on the current HTTP request context — especially useful for setting the Location header in a 201 Created response.
        */
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getEventId())
                .toUri();

        return ResponseEntity.created(location).body(created); // 201 + Location
    }

    //Get /api/events/{id}
    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDto> getEvent(@PathVariable("id") Long eventId) {
        log.info("GET /api/events/{}", eventId);
        Optional<EventDetailDto> found = eventService.getEventById(eventId);
        //Return =
        //If the Optional contains an event, wrap it in ResponseEntity.ok(...) → HTTP 200.
        //If it’s empty, return ResponseEntity.notFound().build() → HTTP 404.
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    //Put /api/events/{id}
    @PutMapping("/{id}")
    public ResponseEntity<EventDetailDto> updateEvent(@PathVariable("id") Long eventId, @RequestBody @Valid UpdateEventDto dto) {
        log.info("PUT /api/events/{}", eventId);
        EventDetailDto updated = eventService.updateEvent(eventId, dto);
        return ResponseEntity.ok(updated);
    }

    //Delete /api/events/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") Long eventId) {
        log.info("DELETE /api/events/{}", eventId);
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

}
