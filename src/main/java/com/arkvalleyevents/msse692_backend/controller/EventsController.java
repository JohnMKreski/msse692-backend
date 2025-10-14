package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/api/events")
public class EventsController {
    private static final Logger logger = LoggerFactory.getLogger(EventsController.class);

    //Service
    private final EventService eventService;

    public EventsController(EventService eventService) {
        this.eventService = eventService;
        logger.info("EventsController initialized");
    }

    private HttpServletRequest request;

    //Post /api/events
    @PostMapping
    public ResponseEntity<EventDetailDto> createEvent(@RequestBody CreateEventDto dto) {
        logger.info("POST /api/events called");
        EventDetailDto created = eventService.createEvent(dto);
        //return ResponseEntity.ok(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

//    // GET /api/events/{id}
//    @GetMapping("/{id}")
//    public ResponseEntity<EventDetailDto> getEvent(@PathVariable Long eventId) {
//        logger.info("GET /api/events/{}", eventId);
//        Optional<EventDetailDto> found = eventService.getEventById(eventId);
//        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
//    }

//    @PutMapping("/{id}")
//    public EventDetailDto update(@PathVariable Long id, @RequestBody @Valid UpdateEventDto dto) {
//        return eventService.updateEvent(id, dto);
//    }
}
